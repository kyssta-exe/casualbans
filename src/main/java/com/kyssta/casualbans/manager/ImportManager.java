package com.kyssta.casualbans.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.UUIDUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles importing punishments from external sources:
 * <ul>
 *   <li>LiteBans (SQL database)</li>
 *   <li>AdvancedBan (SQL database)</li>
 *   <li>Vanilla Minecraft (banned-players.json, banned-ips.json)</li>
 *   <li>CasualBans-compatible JSON export</li>
 * </ul>
 * <p>
 * Each method converts foreign punishment records into {@link Punishment} objects
 * and persists them via {@link CasualBans#getStorageProvider()}.
 */
public class ImportManager {

    private final CasualBans plugin;
    private final Gson gson;

    public ImportManager(CasualBans plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Start an import asynchronously. Calls the progress callback with updates
     * and a final summary on completion.
     *
     * @param source   the import source to use
     * @param callback consumer that receives {@link ImportProgress} updates
     */
    public void startImport(ImportSource source, Consumer<ImportProgress> callback) {
        plugin.getAsyncExecutor().submit(() -> {
            try {
                ImportProgress progress;
                switch (source.type()) {
                    case LITEBANS -> progress = importFromLiteBans(source.sqlConfig());
                    case ADVANCEDBAN -> progress = importFromAdvancedBan(source.sqlConfig());
                    case VANILLA -> progress = importFromVanilla();
                    case JSON -> progress = importFromJson(source.file());
                    default -> {
                        callback.accept(ImportProgress.error("Unknown import source: " + source.type()));
                        return;
                    }
                }
                callback.accept(progress);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Import failed unexpectedly", e);
                callback.accept(ImportProgress.error("Import failed: " + e.getMessage()));
            }
        });
    }

    // ── LiteBans ─────────────────────────────────────────────────────────────

    /**
     * Import punishments from a LiteBans SQL database.
     * Reads from {@code litebans_bans}, {@code litebans_mutes},
     * {@code litebans_warnings}, and {@code litebans_kicks}.
     *
     * @param config connection details for the LiteBans database
     * @return progress result with imported/skipped counts
     */
    public ImportProgress importFromLiteBans(SqlConfig config) {
        plugin.getLogger().info("Starting LiteBans import from " + config.host() + ":" + config.port());

        List<String> warnings = new ArrayList<>();
        int totalImported = 0;
        int totalSkipped = 0;

        try (HikariDataSource ds = createDataSource(config)) {
            // Test connection
            try (Connection c = ds.getConnection()) {
                // just validates
            }

            // Import from each table type
            ImportProgress bans = importLiteBansTable(ds, "litebans_bans", "BAN", null, warnings);
            totalImported += bans.imported();
            totalSkipped += bans.skipped();

            ImportProgress tempBans = importLiteBansTable(ds, "litebans_bans", "TEMPBAN",
                    rs -> { try { return rs.getBoolean("active"); } catch (SQLException e) { return false; } }, warnings);
            totalImported += tempBans.imported();
            totalSkipped += tempBans.skipped();

            ImportProgress mutes = importLiteBansTable(ds, "litebans_mutes", "MUTE", null, warnings);
            totalImported += mutes.imported();
            totalSkipped += mutes.skipped();

            ImportProgress tempMutes = importLiteBansTable(ds, "litebans_mutes", "TEMPMUTE",
                    rs -> { try { return rs.getBoolean("active"); } catch (SQLException e) { return false; } }, warnings);
            totalImported += tempMutes.imported();
            totalSkipped += tempMutes.skipped();

            ImportProgress warns = importLiteBansTable(ds, "litebans_warnings", "WARN", null, warnings);
            totalImported += warns.imported();
            totalSkipped += warns.skipped();

            ImportProgress kicks = importLiteBansTable(ds, "litebans_kicks", "KICK", null, warnings);
            totalImported += kicks.imported();
            totalSkipped += kicks.skipped();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "LiteBans import failed", e);
            return ImportProgress.error("LiteBans import failed: " + e.getMessage());
        }

        return ImportProgress.completed("LiteBans", totalImported, totalSkipped, warnings);
    }

    @FunctionalInterface
    private interface ResultSetBooleanExtractor {
        boolean extract(ResultSet rs) throws SQLException;
    }

    private ImportProgress importLiteBansTable(HikariDataSource ds, String table, String typeStr,
                                                ResultSetBooleanExtractor activeOverride,
                                                List<String> warnings) {
        int imported = 0;
        int skipped = 0;

        String sql = "SELECT id, uuid, ip, reason, "
                + "banned_by_uuid, banned_by_name, "
                + "muted_by_uuid, muted_by_name, "
                + "warned_by_uuid, warned_by_name, "
                + "kicked_by_uuid, kicked_by_name, "
                + "`time`, `until`, active, server_origin, silent "
                + "FROM " + table;

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    Punishment punishment = mapLiteBansRow(rs, typeStr, activeOverride);
                    if (punishment != null) {
                        plugin.getStorageProvider().savePunishment(punishment);
                        imported++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    warnings.add("Row " + getSafeLong(rs, "id") + " in " + table + ": " + e.getMessage());
                    skipped++;
                }
            }
        } catch (SQLException e) {
            // Table may not exist — that's fine for optional tables
            plugin.getLogger().info("Table " + table + " not found or not readable: " + e.getMessage());
        }

        return new ImportProgress("LiteBans", imported, skipped, 0, warnings, false);
    }

    private Punishment mapLiteBansRow(ResultSet rs, String typeStr,
                                       ResultSetBooleanExtractor activeOverride) throws SQLException {
        UUID uuid = parseUuid(rs.getString("uuid"));
        if (uuid == null) return null;

        long id = rs.getLong("id");
        String ip = rs.getString("ip");
        String reason = rs.getString("reason");
        if (reason == null || reason.isEmpty()) reason = "Imported from LiteBans";

        long time = rs.getLong("time");
        long until = rs.getLong("until");

        // Determine if active
        boolean active;
        if (activeOverride != null) {
            active = activeOverride.extract(rs);
        } else {
            active = rs.getBoolean("active");
        }
        // If it's a temp punishment that has expired, mark inactive
        if (until > 0 && until <= System.currentTimeMillis()) {
            active = false;
        }

        // Resolve executor UUID from the first non-null column
        UUID executorUUID = null;
        String executorName = null;

        for (String col : new String[]{"banned_by_uuid", "muted_by_uuid", "warned_by_uuid", "kicked_by_uuid"}) {
            try {
                String val = rs.getString(col);
                if (val != null && !val.isEmpty()) {
                    executorUUID = parseUuid(val);
                    break;
                }
            } catch (SQLException ignored) {
            }
        }

        for (String col : new String[]{"banned_by_name", "muted_by_name", "warned_by_name", "kicked_by_name"}) {
            try {
                String val = rs.getString(col);
                if (val != null && !val.isEmpty()) {
                    executorName = val;
                    break;
                }
            } catch (SQLException ignored) {
            }
        }

        if (executorUUID == null) {
            executorUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        if (executorName == null) {
            executorName = "Console";
        }

        String serverOrigin = rs.getString("server_origin");
        if (serverOrigin == null || serverOrigin.isEmpty()) {
            serverOrigin = plugin.getServerName();
        }

        boolean silent = false;
        try {
            silent = rs.getBoolean("silent");
        } catch (SQLException ignored) {
        }

        PunishmentType type = parseLiteBansType(typeStr, until);
        long duration = until > 0 ? until - time : 0;

        // Resolve name from UUID
        String name = UUIDUtil.getName(uuid);
        if (name == null || name.equals("Unknown") || name.equals(uuid.toString().substring(0, 8))) {
            name = uuid.toString().substring(0, 8);
        }

        return Punishment.builder()
                .id(id)
                .type(type)
                .uuid(uuid)
                .ip(ip)
                .name(name)
                .reason(reason)
                .executorUUID(executorUUID)
                .executorName(executorName)
                .dateStart(time)
                .dateEnd(until)
                .duration(duration)
                .serverScope(plugin.getDefaultServerScope())
                .serverOrigin(serverOrigin)
                .silent(silent)
                .ipPunish(ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0"))
                .active(active)
                .templateName(null)
                .build();
    }

    private PunishmentType parseLiteBansType(String typeStr, long until) {
        return switch (typeStr.toUpperCase()) {
            case "BAN" -> until > 0 ? PunishmentType.TEMPBAN : PunishmentType.BAN;
            case "TEMPBAN" -> PunishmentType.TEMPBAN;
            case "MUTE" -> until > 0 ? PunishmentType.TEMPMUTE : PunishmentType.MUTE;
            case "TEMPMUTE" -> PunishmentType.TEMPMUTE;
            case "WARN" -> PunishmentType.WARN;
            case "KICK" -> PunishmentType.KICK;
            default -> PunishmentType.BAN;
        };
    }

    // ── AdvancedBan ──────────────────────────────────────────────────────────

    /**
     * Import punishments from an AdvancedBan SQL database.
     * Reads from the {@code punishments} table.
     *
     * @param config connection details for the AdvancedBan database
     * @return progress result with imported/skipped counts
     */
    public ImportProgress importFromAdvancedBan(SqlConfig config) {
        plugin.getLogger().info("Starting AdvancedBan import from " + config.host() + ":" + config.port());

        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try (HikariDataSource ds = createDataSource(config)) {
            // Test connection
            try (Connection c = ds.getConnection()) {
            }

            String sql = "SELECT id, uuid, name, reason, operator, punishmentType, "
                    + "start, `end`, ip, punishedBefore FROM punishments";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    try {
                        Punishment punishment = mapAdvancedBanRow(rs, warnings);
                        if (punishment != null) {
                            plugin.getStorageProvider().savePunishment(punishment);
                            imported++;
                        } else {
                            skipped++;
                        }
                    } catch (Exception e) {
                        warnings.add("Row " + getSafeLong(rs, "id") + ": " + e.getMessage());
                        skipped++;
                    }
                }
            } catch (SQLException e) {
                return ImportProgress.error("AdvancedBan query failed: " + e.getMessage());
            }

        } catch (Exception e) {
            return ImportProgress.error("AdvancedBan import failed: " + e.getMessage());
        }

        return ImportProgress.completed("AdvancedBan", imported, skipped, warnings);
    }

    private Punishment mapAdvancedBanRow(ResultSet rs, List<String> warnings) throws SQLException {
        String uuidStr = rs.getString("uuid");
        UUID uuid = parseUuid(uuidStr);
        if (uuid == null) {
            String name = rs.getString("name");
            if (name != null && !name.isEmpty()) {
                uuid = UUIDUtil.resolveUUID(name);
            }
            if (uuid == null) return null;
        }

        long id = rs.getLong("id");
        String name = rs.getString("name");
        String reason = rs.getString("reason");
        if (reason == null || reason.isEmpty()) reason = "Imported from AdvancedBan";
        String operator = rs.getString("operator");
        String punishmentType = rs.getString("punishmentType");
        long start = rs.getLong("start");
        long end = rs.getLong("end");
        String ip = rs.getString("ip");

        boolean active = end <= 0 || end > System.currentTimeMillis();

        // Resolve executor UUID
        UUID operatorUUID = null;
        String operatorName = "Console";
        if (operator != null && !operator.isEmpty()) {
            operatorName = operator;
            operatorUUID = UUIDUtil.resolveUUID(operator);
            if (operatorUUID == null) {
                operatorUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
            }
        }

        PunishmentType type = mapAdvancedBanType(punishmentType, end);
        long duration = end > 0 ? end - start : 0;

        // Resolve name
        String resolvedName = name;
        if (resolvedName == null || resolvedName.isEmpty()) {
            resolvedName = UUIDUtil.getName(uuid);
        }
        if (resolvedName == null || resolvedName.equals("Unknown")) {
            resolvedName = uuid.toString().substring(0, 8);
        }

        boolean ipPunish = type == PunishmentType.IPBAN || type == PunishmentType.IPMUTE
                || (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0"));

        return Punishment.builder()
                .id(id)
                .type(type)
                .uuid(uuid)
                .ip(ip)
                .name(resolvedName)
                .reason(reason)
                .executorUUID(operatorUUID)
                .executorName(operatorName)
                .dateStart(start)
                .dateEnd(end)
                .duration(duration)
                .serverScope(plugin.getDefaultServerScope())
                .serverOrigin(plugin.getServerName())
                .silent(false)
                .ipPunish(ipPunish)
                .active(active)
                .templateName(null)
                .build();
    }

    private PunishmentType mapAdvancedBanType(String type, long end) {
        if (type == null) return PunishmentType.BAN;
        return switch (type.toUpperCase()) {
            case "BAN", "BANNED" -> end > 0 ? PunishmentType.TEMPBAN : PunishmentType.BAN;
            case "TEMP_BAN", "TEMPBAN", "TEMP BAN" -> PunishmentType.TEMPBAN;
            case "IP_BAN", "IPBAN", "IP BAN" -> PunishmentType.IPBAN;
            case "MUTE", "MUTED" -> end > 0 ? PunishmentType.TEMPMUTE : PunishmentType.MUTE;
            case "TEMP_MUTE", "TEMPMUTE", "TEMP MUTE" -> PunishmentType.TEMPMUTE;
            case "IP_MUTE", "IPMUTE", "IP MUTE" -> PunishmentType.IPMUTE;
            case "WARNING", "WARN" -> PunishmentType.WARN;
            case "KICK", "KICKED" -> PunishmentType.KICK;
            default -> PunishmentType.BAN;
        };
    }

    // ── Vanilla Minecraft ────────────────────────────────────────────────────

    /**
     * Import punishments from the vanilla Minecraft server files
     * ({@code banned-players.json} and {@code banned-ips.json}).
     * <p>
     * These files are located in the server root directory (the working directory).
     *
     * @return progress result with imported/skipped counts
     */
    public ImportProgress importFromVanilla() {
        plugin.getLogger().info("Starting vanilla Minecraft import...");

        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        // Vanilla files are in the server root (working directory)
        File serverDir = new File(".");

        // Import banned-players.json
        File bannedPlayers = new File(serverDir, "banned-players.json");
        if (bannedPlayers.exists()) {
            ImportProgress playerProgress = importVanillaBannedPlayers(bannedPlayers, warnings);
            imported += playerProgress.imported();
            skipped += playerProgress.skipped();
            warnings.addAll(playerProgress.warnings());
        } else {
            warnings.add("banned-players.json not found in server directory");
        }

        // Import banned-ips.json
        File bannedIps = new File(serverDir, "banned-ips.json");
        if (bannedIps.exists()) {
            ImportProgress ipProgress = importVanillaBannedIps(bannedIps, warnings);
            imported += ipProgress.imported();
            skipped += ipProgress.skipped();
            warnings.addAll(ipProgress.warnings());
        } else {
            warnings.add("banned-ips.json not found in server directory");
        }

        return ImportProgress.completed("Vanilla", imported, skipped, warnings);
    }

    private ImportProgress importVanillaBannedPlayers(File file, List<String> warnings) {
        int imported = 0;
        int skipped = 0;

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<VanillaBanEntry>>() {}.getType();
            List<VanillaBanEntry> entries = gson.fromJson(reader, listType);

            if (entries == null) return new ImportProgress("Vanilla", 0, 0, 0, warnings, false);

            for (VanillaBanEntry entry : entries) {
                try {
                    UUID uuid = parseUuid(entry.uuid);
                    if (uuid == null) {
                        skipped++;
                        continue;
                    }

                    String reason = entry.reason;
                    if (reason == null || reason.isEmpty()) reason = "Imported from vanilla";
                    long created = entry.created;
                    long expires = entry.expires;

                    // Vanilla uses -1 for permanent bans
                    if (expires <= 0) expires = 0;

                    boolean active = expires <= 0 || expires > System.currentTimeMillis();

                    String source = entry.source;
                    if (source == null || source.isEmpty()) source = "Vanilla";

                    UUID executorUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
                    if (source != null && !source.isEmpty() && !source.equals("Vanilla")) {
                        UUID resolved = UUIDUtil.resolveUUID(source);
                        if (resolved != null) executorUUID = resolved;
                    }

                    PunishmentType type = expires > 0 ? PunishmentType.TEMPBAN : PunishmentType.BAN;
                    long duration = expires > 0 ? expires - created : 0;

                    String resolvedName = UUIDUtil.getName(uuid);
                    if (resolvedName == null || resolvedName.equals("Unknown")) {
                        resolvedName = uuid.toString().substring(0, 8);
                    }

                    Punishment punishment = Punishment.builder()
                            .type(type)
                            .uuid(uuid)
                            .name(resolvedName)
                            .reason(reason)
                            .executorUUID(executorUUID)
                            .executorName(source)
                            .dateStart(created)
                            .dateEnd(expires)
                            .duration(duration)
                            .serverScope(plugin.getDefaultServerScope())
                            .serverOrigin(plugin.getServerName())
                            .silent(false)
                            .ipPunish(false)
                            .active(active)
                            .templateName(null)
                            .build();

                    plugin.getStorageProvider().savePunishment(punishment);
                    imported++;

                } catch (Exception e) {
                    warnings.add("banned-players entry '" + entry.uuid + "': " + e.getMessage());
                    skipped++;
                }
            }
        } catch (IOException e) {
            warnings.add("Cannot read banned-players.json: " + e.getMessage());
        }

        return new ImportProgress("Vanilla", imported, skipped, 0, warnings, false);
    }

    private ImportProgress importVanillaBannedIps(File file, List<String> warnings) {
        int imported = 0;
        int skipped = 0;

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<VanillaIpBanEntry>>() {}.getType();
            List<VanillaIpBanEntry> entries = gson.fromJson(reader, listType);

            if (entries == null) return new ImportProgress("Vanilla", 0, 0, 0, warnings, false);

            for (VanillaIpBanEntry entry : entries) {
                try {
                    String ip = entry.ip;
                    if (ip == null || ip.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    String reason = entry.reason;
                    if (reason == null || reason.isEmpty()) reason = "Imported from vanilla";
                    long created = entry.created;
                    long expires = entry.expires;

                    if (expires <= 0) expires = 0;

                    boolean active = expires <= 0 || expires > System.currentTimeMillis();

                    String source = entry.source;
                    if (source == null || source.isEmpty()) source = "Vanilla";

                    UUID executorUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
                    if (source != null && !source.isEmpty() && !source.equals("Vanilla")) {
                        UUID resolved = UUIDUtil.resolveUUID(source);
                        if (resolved != null) executorUUID = resolved;
                    }

                    PunishmentType type = PunishmentType.IPBAN;
                    long duration = expires > 0 ? expires - created : 0;

                    Punishment punishment = Punishment.builder()
                            .type(type)
                            .uuid(null)
                            .ip(ip)
                            .name(ip)
                            .reason(reason)
                            .executorUUID(executorUUID)
                            .executorName(source)
                            .dateStart(created)
                            .dateEnd(expires)
                            .duration(duration)
                            .serverScope(plugin.getDefaultServerScope())
                            .serverOrigin(plugin.getServerName())
                            .silent(false)
                            .ipPunish(true)
                            .active(active)
                            .templateName(null)
                            .build();

                    plugin.getStorageProvider().savePunishment(punishment);
                    imported++;

                } catch (Exception e) {
                    warnings.add("banned-ips entry '" + entry.ip + "': " + e.getMessage());
                    skipped++;
                }
            }
        } catch (IOException e) {
            warnings.add("Cannot read banned-ips.json: " + e.getMessage());
        }

        return new ImportProgress("Vanilla", imported, skipped, 0, warnings, false);
    }

    // ── JSON Import ──────────────────────────────────────────────────────────

    /**
     * Import punishments from a CasualBans-compatible JSON export file.
     * <p>
     * Expected format: a JSON array of {@link Punishment} objects, or a single
     * object with a {@code "punishments"} key containing the array.
     *
     * @param file the JSON file to import
     * @return progress result with imported/skipped counts
     */
    public ImportProgress importFromJson(File file) {
        plugin.getLogger().info("Starting JSON import from " + file.getAbsolutePath());

        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        if (!file.exists()) {
            return ImportProgress.error("File not found: " + file.getAbsolutePath());
        }

        try {
            // Try reading as a plain array first
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            content = content.trim();

            List<Punishment> punishments = null;

            // Plain array format: [...]
            if (content.startsWith("[")) {
                Type listType = new TypeToken<List<Punishment>>() {}.getType();
                punishments = gson.fromJson(content, listType);
            }
            // Wrapper format: { "punishments": [...] }
            else if (content.startsWith("{")) {
                JsonImportWrapper wrapper = gson.fromJson(content, JsonImportWrapper.class);
                if (wrapper != null) {
                    punishments = wrapper.punishments;
                }
            }

            if (punishments == null || punishments.isEmpty()) {
                return ImportProgress.completed("JSON", 0, 0, warnings);
            }

            for (Punishment p : punishments) {
                int result = saveImportedPunishment(p, warnings);
                if (result > 0) {
                    imported++;
                } else {
                    skipped++;
                }
            }

        } catch (Exception e) {
            return ImportProgress.error("JSON import failed: " + e.getMessage());
        }

        return ImportProgress.completed("JSON", imported, skipped, warnings);
    }

    /**
     * Save a single imported punishment, setting defaults for null fields.
     *
     * @return 1 if saved, 0 if skipped
     */
    private int saveImportedPunishment(Punishment p, List<String> warnings) {
        try {
            if (p == null) return 0;
            if (p.getType() == null) {
                warnings.add("Skipped punishment with null type");
                return 0;
            }
            if (p.getUuid() == null && (p.getIp() == null || p.getIp().isEmpty())) {
                warnings.add("Skipped punishment with no UUID and no IP");
                return 0;
            }

            // Set defaults for missing fields
            if (p.getDateStart() <= 0) p.setDateStart(System.currentTimeMillis());
            if (p.getReason() == null || p.getReason().isEmpty()) p.setReason("Imported");
            if (p.getExecutorUUID() == null)
                p.setExecutorUUID(UUID.fromString("00000000-0000-0000-0000-000000000000"));
            if (p.getExecutorName() == null || p.getExecutorName().isEmpty()) p.setExecutorName("Console");
            if (p.getServerOrigin() == null || p.getServerOrigin().isEmpty())
                p.setServerOrigin(plugin.getServerName());
            if (p.getServerScope() == null || p.getServerScope().isEmpty())
                p.setServerScope(plugin.getDefaultServerScope());
            if (p.getName() == null || p.getName().isEmpty()) {
                if (p.getUuid() != null) {
                    String name = UUIDUtil.getName(p.getUuid());
                    p.setName(name != null ? name : p.getUuid().toString().substring(0, 8));
                } else {
                    p.setName(p.getIp());
                }
            }

            p.setId(0); // Let storage assign a new ID
            plugin.getStorageProvider().savePunishment(p);
            return 1;
        } catch (Exception e) {
            warnings.add("Error saving punishment: " + e.getMessage());
            return 0;
        }
    }

    // ── SQL Configuration ────────────────────────────────────────────────────

    /**
     * Connection configuration for an external SQL database.
     */
    public record SqlConfig(
            String host,
            int port,
            String database,
            String username,
            String password
    ) {
        public SqlConfig {
            if (host == null || host.isBlank()) host = "localhost";
            if (port <= 0) port = 3306;
            if (database == null || database.isBlank()) database = "minecraft";
            if (username == null) username = "root";
            if (password == null) password = "";
        }
    }

    // ── Import Source ────────────────────────────────────────────────────────

    /**
     * Describes what to import and where the data lives.
     */
    public record ImportSource(
            SourceType type,
            SqlConfig sqlConfig,
            File file
    ) {
        public enum SourceType {
            LITEBANS,
            ADVANCEDBAN,
            VANILLA,
            JSON
        }

        public static ImportSource liteBans(SqlConfig config) {
            return new ImportSource(SourceType.LITEBANS, config, null);
        }

        public static ImportSource advancedBan(SqlConfig config) {
            return new ImportSource(SourceType.ADVANCEDBAN, config, null);
        }

        public static ImportSource vanilla() {
            return new ImportSource(SourceType.VANILLA, null, null);
        }

        public static ImportSource json(File file) {
            return new ImportSource(SourceType.JSON, null, file);
        }
    }

    // ── Progress Reporting ───────────────────────────────────────────────────

    /**
     * Reports the result of an import operation.
     */
    public record ImportProgress(
            String sourceName,
            int imported,
            int skipped,
            long elapsedMs,
            List<String> warnings,
            boolean error,
            String errorMessage
    ) {
        /**
         * Convenience constructor for intermediate progress without a final error.
         */
        public ImportProgress(String sourceName, int imported, int skipped, long elapsedMs,
                              List<String> warnings, boolean error) {
            this(sourceName, imported, skipped, elapsedMs,
                    warnings != null ? warnings : new ArrayList<>(),
                    error, error ? "Unknown error" : null);
        }

        public static ImportProgress completed(String sourceName, int imported, int skipped,
                                                List<String> warnings) {
            return new ImportProgress(sourceName, imported, skipped, 0,
                    warnings, false);
        }

        public static ImportProgress error(String message) {
            return new ImportProgress("", 0, 0, 0,
                    new ArrayList<>(), true, message);
        }

        /**
         * @return a human-readable summary of this import result
         */
        public String toSummary() {
            StringBuilder sb = new StringBuilder();
            if (error) {
                sb.append("<red>Import failed: ").append(errorMessage).append("</red>");
                return sb.toString();
            }
            sb.append("<green>Import from ").append(sourceName).append(" completed.</green>\n");
            sb.append("<gray>  Imported: </gray><green>").append(imported).append("</green>\n");
            sb.append("<gray>  Skipped: </gray><yellow>").append(skipped).append("</yellow>");
            if (elapsedMs > 0) {
                sb.append("\n<gray>  Time: </gray><white>").append(elapsedMs).append("ms</white>");
            }
            if (warnings != null && !warnings.isEmpty()) {
                sb.append("\n<yellow>  Warnings (").append(warnings.size()).append("):</yellow>");
                int shown = 0;
                for (String w : warnings) {
                    if (shown >= 10) {
                        sb.append("\n<gray>    ... and ").append(warnings.size() - shown).append(" more</gray>");
                        break;
                    }
                    sb.append("\n<dark_gray>    • </dark_gray><gray>").append(w).append("</gray>");
                    shown++;
                }
            }
            return sb.toString();
        }
    }

    // ── JSON model classes for vanilla files ─────────────────────────────────

    @SuppressWarnings("unused")
    private static class VanillaBanEntry {
        String uuid;
        String name;
        String reason;
        String source;
        long created;
        long expires;
    }

    @SuppressWarnings("unused")
    private static class VanillaIpBanEntry {
        String ip;
        String reason;
        String source;
        long created;
        long expires;
    }

    @SuppressWarnings("unused")
    private static class JsonImportWrapper {
        List<Punishment> punishments;
    }

    // ── Utils ────────────────────────────────────────────────────────────────

    private HikariDataSource createDataSource(SqlConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("CasualBans-Import");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.host() + ":" + config.port()
                + "/" + config.database() + "?useSSL=false&characterEncoding=utf8&serverTimezone=UTC");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setConnectionTimeout(15000);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaxLifetime(60000);
        return new HikariDataSource(hikariConfig);
    }

    private static UUID parseUuid(String input) {
        if (input == null || input.isEmpty()) return null;
        try {
            if (input.contains("-")) {
                return UUID.fromString(input);
            }
            // 32-char hex without dashes
            if (input.length() == 32) {
                String withDashes = input.replaceAll(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                );
                return UUID.fromString(withDashes);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private static long getSafeLong(ResultSet rs, String column) {
        try {
            return rs.getLong(column);
        } catch (SQLException e) {
            return -1;
        }
    }
}
