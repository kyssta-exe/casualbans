package com.kyssta.casualbans.storage;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.model.StaffNote;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * SQL-backed implementation of {@link StorageProvider} using HikariCP.
 * Supports H2, MySQL, MariaDB, and PostgreSQL.
 * Table names use the configurable prefix from {@code storage.sql.table-prefix}
 * (default {@code cb_}).
 * All queries use {@link PreparedStatement} for SQL injection safety.
 */
public class SqlStorage implements StorageProvider {

    private enum DbFlavor { MYSQL, MARIADB, POSTGRESQL, H2 }

    private final CasualBans plugin;
    private final String tablePrefix;
    private final DbFlavor flavor;

    private HikariDataSource dataSource;
    private final AtomicLong nextPunishmentId = new AtomicLong(-1);

    public SqlStorage(CasualBans plugin) {
        this.plugin = plugin;
        this.tablePrefix = plugin.getConfig().getString("storage.sql.table-prefix", "cb_");
        this.flavor = parseFlavor(plugin.getConfig().getString("storage.driver", "H2"));
    }

    private static DbFlavor parseFlavor(String driver) {
        switch (driver.toUpperCase()) {
            case "MYSQL":    return DbFlavor.MYSQL;
            case "MARIADB":  return DbFlavor.MARIADB;
            case "POSTGRESQL": return DbFlavor.POSTGRESQL;
            case "H2":
            default:         return DbFlavor.H2;
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void initialize() {
        createDataSource();
        initTables();
        seedNextId();
        seedNextNoteId();
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed() && dataSource.isRunning();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /** Reinitialises the connection pool (used on config reload). */
    public void reconnect() {
        shutdown();
        initialize();
    }

    // ── Connection Pool ──────────────────────────────────────────────────────

    private void createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("CasualBans-Hikari");
        config.setJdbcUrl(buildJdbcUrl());
        config.setDriverClassName(driverClass());
        config.setUsername(plugin.getConfig().getString("storage.sql.username", "root"));
        config.setPassword(plugin.getConfig().getString("storage.sql.password", ""));
        config.setMinimumIdle(plugin.getConfig().getInt("storage.sql.pool.min-connections", 2));
        config.setMaximumPoolSize(plugin.getConfig().getInt("storage.sql.pool.max-connections", 10));
        config.setConnectionTimeout(plugin.getConfig().getLong("storage.sql.pool.timeout", 30000));
        config.setIdleTimeout(plugin.getConfig().getLong("storage.sql.pool.idle-timeout", 600000));
        config.setMaxLifetime(plugin.getConfig().getLong("storage.sql.pool.max-lifetime", 1800000));

        if (flavor == DbFlavor.H2) {
            config.addDataSourceProperty("DB_CLOSE_DELAY", "-1");
            config.addDataSourceProperty("DB_CLOSE_ON_EXIT", "false");
            config.addDataSourceProperty("MODE", "MySQL");
        }

        this.dataSource = new HikariDataSource(config);
    }

    private String buildJdbcUrl() {
        String host = plugin.getConfig().getString("storage.sql.host", "localhost");
        int port = plugin.getConfig().getInt("storage.sql.port", flavor == DbFlavor.POSTGRESQL ? 5432 : 3306);
        String database = plugin.getConfig().getString("storage.sql.database", "casualbans");

        switch (flavor) {
            case MYSQL:
            case MARIADB:
                return "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&characterEncoding=utf8&serverTimezone=UTC";
            case POSTGRESQL:
                return "jdbc:postgresql://" + host + ":" + port + "/" + database;
            case H2:
            default:
                return "jdbc:h2:file:" + plugin.getDataFolder().toPath().resolve("data").resolve("database")
                        + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL";
        }
    }

    private String driverClass() {
        switch (flavor) {
            case MYSQL:
            case MARIADB:
                return "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case H2:
            default:
                return "org.h2.Driver";
        }
    }

    // ── Table Initialisation ─────────────────────────────────────────────────

    private void initTables() {
        String p = tablePrefix;

        execUpdate("CREATE TABLE IF NOT EXISTS " + p + "punishments ("
                + "id BIGINT PRIMARY KEY,"
                + "type VARCHAR(20) NOT NULL,"
                + "uuid VARCHAR(36) NOT NULL,"
                + "ip VARCHAR(45),"
                + "name VARCHAR(16),"
                + "reason TEXT,"
                + "executor_uuid VARCHAR(36),"
                + "executor_name VARCHAR(16),"
                + "removed_by_uuid VARCHAR(36),"
                + "removed_by_name VARCHAR(16),"
                + "removal_reason TEXT,"
                + "date_start BIGINT NOT NULL,"
                + "date_end BIGINT NOT NULL,"
                + "server_scope VARCHAR(64),"
                + "server_origin VARCHAR(64),"
                + "silent BOOLEAN DEFAULT FALSE,"
                + "ip_punish BOOLEAN DEFAULT FALSE,"
                + "active BOOLEAN DEFAULT TRUE,"
                + "template_name VARCHAR(64),"
                + "duration BIGINT DEFAULT 0"
                + ")");

        execUpdate("CREATE TABLE IF NOT EXISTS " + p + "ip_history ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "player_name VARCHAR(16) NOT NULL,"
                + "ip VARCHAR(45) NOT NULL,"
                + "timestamp BIGINT NOT NULL"
                + ")");

        execUpdate("CREATE TABLE IF NOT EXISTS " + p + "name_history ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "old_name VARCHAR(16),"
                + "new_name VARCHAR(16) NOT NULL,"
                + "timestamp BIGINT NOT NULL"
                + ")");

        execUpdate("CREATE TABLE IF NOT EXISTS " + p + "staff_notes ("
                + "id BIGINT PRIMARY KEY,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "target_name VARCHAR(16) NOT NULL,"
                + "note TEXT NOT NULL,"
                + "author_uuid VARCHAR(36) NOT NULL,"
                + "author_name VARCHAR(16) NOT NULL,"
                + "timestamp BIGINT NOT NULL"
                + ")");

        // Indexes (silently ignore if already exist)
        tryCreateIndex(p + "punishments_uuid_idx", "CREATE INDEX ON " + p + "punishments(uuid)");
        tryCreateIndex(p + "punishments_active_idx", "CREATE INDEX ON " + p + "punishments(active)");
        tryCreateIndex(p + "punishments_executor_idx", "CREATE INDEX ON " + p + "punishments(executor_uuid)");
        tryCreateIndex(p + "ip_history_ip_idx", "CREATE INDEX ON " + p + "ip_history(ip)");
        tryCreateIndex(p + "ip_history_uuid_idx", "CREATE INDEX ON " + p + "ip_history(uuid)");
        tryCreateIndex(p + "name_history_uuid_idx", "CREATE INDEX ON " + p + "name_history(uuid)");
        tryCreateIndex(p + "staff_notes_target_idx", "CREATE INDEX ON " + p + "staff_notes(target_uuid)");
    }

    private void tryCreateIndex(String indexName, String sql) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            if (flavor == DbFlavor.MYSQL || flavor == DbFlavor.MARIADB) {
                stmt.execute("CREATE INDEX " + indexName + " ON " + tablePrefix
                        + sql.substring(sql.indexOf("ON ") + 3));
            } else {
                stmt.execute(sql);
            }
        } catch (SQLException ignored) {
            // Index already exists — safe to ignore
        }
    }

    private void seedNextId() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + tablePrefix + "punishments")) {
            if (rs.next()) {
                nextPunishmentId.set(rs.getLong(1) + 1);
            }
        } catch (SQLException e) {
            nextPunishmentId.set(1);
        }
    }

    private final AtomicLong nextNoteId = new AtomicLong(-1);

    private void seedNextNoteId() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + tablePrefix + "staff_notes")) {
            if (rs.next()) {
                nextNoteId.set(rs.getLong(1) + 1);
            }
        } catch (SQLException e) {
            nextNoteId.set(1);
        }
    }

    // ── Punishment CRUD ──────────────────────────────────────────────────────

    @Override
    public void savePunishment(Punishment punishment) {
        if (punishment.getId() > 0 && getPunishment(punishment.getId()) != null) {
            updatePunishment(punishment);
            return;
        }
        if (punishment.getId() <= 0) {
            punishment.setId(nextPunishmentId.getAndIncrement());
        } else {
            long id = punishment.getId();
            if (id >= nextPunishmentId.get()) {
                nextPunishmentId.set(id + 1);
            }
        }
        String sql = "INSERT INTO " + tablePrefix + "punishments ("
                + "id, type, uuid, ip, name, reason, executor_uuid, executor_name, "
                + "removed_by_uuid, removed_by_name, removal_reason, date_start, date_end, "
                + "server_scope, server_origin, silent, ip_punish, active, template_name, duration"
                + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        execUpdate(sql, stmt -> {
            stmt.setLong(1, punishment.getId());
            stmt.setString(2, punishment.getType().name());
            stmt.setString(3, punishment.getUuid().toString());
            stmt.setString(4, punishment.getIp());
            stmt.setString(5, punishment.getName());
            stmt.setString(6, punishment.getReason());
            setUuid(stmt, 7, punishment.getExecutorUUID());
            stmt.setString(8, punishment.getExecutorName());
            setUuid(stmt, 9, punishment.getRemovedByUUID());
            stmt.setString(10, punishment.getRemovedByName());
            stmt.setString(11, punishment.getRemovalReason());
            stmt.setLong(12, punishment.getDateStart());
            stmt.setLong(13, punishment.getDateEnd());
            stmt.setString(14, punishment.getServerScope());
            stmt.setString(15, punishment.getServerOrigin());
            stmt.setBoolean(16, punishment.isSilent());
            stmt.setBoolean(17, punishment.isIpPunish());
            stmt.setBoolean(18, punishment.isActive());
            stmt.setString(19, punishment.getTemplateName());
            stmt.setLong(20, punishment.getDuration());
        });
    }

    @Override
    public void removePunishment(long id) {
        execUpdate("DELETE FROM " + tablePrefix + "punishments WHERE id = ?",
                stmt -> stmt.setLong(1, id));
    }

    @Override
    public void updatePunishment(Punishment punishment) {
        String sql = "UPDATE " + tablePrefix + "punishments SET "
                + "type=?, uuid=?, ip=?, name=?, reason=?, executor_uuid=?, executor_name=?, "
                + "removed_by_uuid=?, removed_by_name=?, removal_reason=?, date_start=?, date_end=?, "
                + "server_scope=?, server_origin=?, silent=?, ip_punish=?, active=?, template_name=?, duration=? "
                + "WHERE id=?";
        execUpdate(sql, stmt -> {
            stmt.setString(1, punishment.getType().name());
            stmt.setString(2, punishment.getUuid().toString());
            stmt.setString(3, punishment.getIp());
            stmt.setString(4, punishment.getName());
            stmt.setString(5, punishment.getReason());
            setUuid(stmt, 6, punishment.getExecutorUUID());
            stmt.setString(7, punishment.getExecutorName());
            setUuid(stmt, 8, punishment.getRemovedByUUID());
            stmt.setString(9, punishment.getRemovedByName());
            stmt.setString(10, punishment.getRemovalReason());
            stmt.setLong(11, punishment.getDateStart());
            stmt.setLong(12, punishment.getDateEnd());
            stmt.setString(13, punishment.getServerScope());
            stmt.setString(14, punishment.getServerOrigin());
            stmt.setBoolean(15, punishment.isSilent());
            stmt.setBoolean(16, punishment.isIpPunish());
            stmt.setBoolean(17, punishment.isActive());
            stmt.setString(18, punishment.getTemplateName());
            stmt.setLong(19, punishment.getDuration());
            stmt.setLong(20, punishment.getId());
        });
    }

    @Override
    public Punishment getPunishment(long id) {
        return queryOne("SELECT * FROM " + tablePrefix + "punishments WHERE id = ?",
                stmt -> stmt.setLong(1, id), this::mapPunishment);
    }

    // ── Active Punishment Queries ────────────────────────────────────────────

    @Override
    public List<Punishment> getActivePunishments(UUID uuid, PunishmentType type, String serverScope) {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tablePrefix
                + "punishments WHERE uuid=? AND active=TRUE");
        List<Object> params = new ArrayList<>();
        params.add(uuid.toString());
        if (type != null) {
            sql.append(" AND type=?");
            params.add(type.name());
        }
        appendScope(sql, params, serverScope);
        sql.append(" AND (date_end<=0 OR date_end>?)");
        params.add(System.currentTimeMillis());
        sql.append(" ORDER BY date_start DESC");
        return queryList(sql.toString(), stmt -> bindParams(stmt, params), this::mapPunishment);
    }

    @Override
    public List<Punishment> getActivePunishmentsByIP(String ip, PunishmentType type, String serverScope) {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tablePrefix
                + "punishments WHERE ip=? AND active=TRUE");
        List<Object> params = new ArrayList<>();
        params.add(ip);
        if (type != null) {
            sql.append(" AND type=?");
            params.add(type.name());
        }
        appendScope(sql, params, serverScope);
        sql.append(" AND (date_end<=0 OR date_end>?)");
        params.add(System.currentTimeMillis());
        sql.append(" ORDER BY date_start DESC");
        return queryList(sql.toString(), stmt -> bindParams(stmt, params), this::mapPunishment);
    }

    @Override
    public List<Punishment> getAllActivePunishments(PunishmentType type, String serverScope) {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tablePrefix
                + "punishments WHERE active=TRUE");
        List<Object> params = new ArrayList<>();
        if (type != null) {
            sql.append(" AND type=?");
            params.add(type.name());
        }
        appendScope(sql, params, serverScope);
        sql.append(" AND (date_end<=0 OR date_end>?)");
        params.add(System.currentTimeMillis());
        sql.append(" ORDER BY date_start DESC");
        return queryList(sql.toString(), stmt -> bindParams(stmt, params), this::mapPunishment);
    }

    @Override
    public boolean isPlayerBanned(UUID uuid, String serverScope) {
        return hasActivePunishmentOfTypes(uuid, serverScope,
                PunishmentType.BAN, PunishmentType.TEMPBAN, PunishmentType.IPBAN);
    }

    @Override
    public boolean isPlayerMuted(UUID uuid, String serverScope) {
        return hasActivePunishmentOfTypes(uuid, serverScope,
                PunishmentType.MUTE, PunishmentType.TEMPMUTE, PunishmentType.IPMUTE);
    }

    @Override
    public boolean isPlayerWarned(UUID uuid, String serverScope) {
        return hasActivePunishmentOfTypes(uuid, serverScope, PunishmentType.WARN);
    }

    private boolean hasActivePunishmentOfTypes(UUID uuid, String serverScope, PunishmentType... types) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tablePrefix + "punishments"
                + " WHERE uuid=? AND active=TRUE AND (date_end<=0 OR date_end>?)");
        List<Object> params = new ArrayList<>();
        params.add(uuid.toString());
        params.add(System.currentTimeMillis());
        sql.append(" AND type IN (");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
            params.add(types[i].name());
        }
        sql.append(")");
        appendScope(sql, params, serverScope);
        return queryOne(sql.toString(), stmt -> bindParams(stmt, params), rs -> rs.getInt(1) > 0);
    }

    // ── History Queries ──────────────────────────────────────────────────────

    @Override
    public List<Punishment> getHistory(UUID uuid, int limit) {
        String sql = "SELECT * FROM " + tablePrefix + "punishments WHERE uuid=? ORDER BY date_start DESC"
                + (limit > 0 ? " LIMIT ?" : "");
        return queryList(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            if (limit > 0) stmt.setInt(2, limit);
        }, this::mapPunishment);
    }

    @Override
    public List<Punishment> getHistoryByType(UUID uuid, PunishmentType type, int limit) {
        String sql = "SELECT * FROM " + tablePrefix + "punishments WHERE uuid=? AND type=? ORDER BY date_start DESC"
                + (limit > 0 ? " LIMIT ?" : "");
        return queryList(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, type.name());
            if (limit > 0) stmt.setInt(3, limit);
        }, this::mapPunishment);
    }

    @Override
    public List<Punishment> getStaffHistory(UUID executorUUID, int limit) {
        String sql = "SELECT * FROM " + tablePrefix + "punishments WHERE executor_uuid=? ORDER BY date_start DESC"
                + (limit > 0 ? " LIMIT ?" : "");
        return queryList(sql, stmt -> {
            stmt.setString(1, executorUUID.toString());
            if (limit > 0) stmt.setInt(2, limit);
        }, this::mapPunishment);
    }

    @Override
    public List<Punishment> getStaffHistoryByType(UUID executorUUID, PunishmentType type, int limit) {
        String sql = "SELECT * FROM " + tablePrefix
                + "punishments WHERE executor_uuid=? AND type=? ORDER BY date_start DESC"
                + (limit > 0 ? " LIMIT ?" : "");
        return queryList(sql, stmt -> {
            stmt.setString(1, executorUUID.toString());
            stmt.setString(2, type.name());
            if (limit > 0) stmt.setInt(3, limit);
        }, this::mapPunishment);
    }

    @Override
    public int getOffenseCount(UUID uuid, String templateName, String serverScope) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tablePrefix + "punishments"
                + " WHERE uuid=? AND template_name=? AND active=TRUE AND (date_end<=0 OR date_end>?)");
        List<Object> params = new ArrayList<>();
        params.add(uuid.toString());
        params.add(templateName);
        params.add(System.currentTimeMillis());
        appendScope(sql, params, serverScope);
        return queryOne(sql.toString(), stmt -> bindParams(stmt, params), rs -> rs.getInt(1));
    }

    @Override
    public int getOffenseCountByIP(String ip, String templateName) {
        String sql = "SELECT COUNT(*) FROM " + tablePrefix + "punishments"
                + " WHERE ip=? AND template_name=? AND active=TRUE AND (date_end<=0 OR date_end>?)";
        return queryOne(sql, stmt -> {
            stmt.setString(1, ip);
            stmt.setString(2, templateName);
            stmt.setLong(3, System.currentTimeMillis());
        }, rs -> rs.getInt(1));
    }

    // ── Alt Detection / IP Tracking ──────────────────────────────────────────

    @Override
    public List<UUID> getAccountsByIP(String ip) {
        String sql = "SELECT DISTINCT uuid FROM " + tablePrefix + "ip_history WHERE ip=?";
        return queryList(sql, stmt -> stmt.setString(1, ip), rs -> UUID.fromString(rs.getString("uuid")));
    }

    @Override
    public List<String> getIPsByUUID(UUID uuid) {
        String sql = "SELECT DISTINCT ip FROM " + tablePrefix + "ip_history WHERE uuid=?";
        return queryList(sql, stmt -> stmt.setString(1, uuid.toString()), rs -> rs.getString("ip"));
    }

    @Override
    public void recordIPAddress(UUID uuid, String ip, String playerName) {
        String sql = "INSERT INTO " + tablePrefix + "ip_history (uuid, player_name, ip, timestamp) VALUES (?,?,?,?)";
        execUpdate(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, ip);
            stmt.setLong(4, System.currentTimeMillis());
        });
    }

    @Override
    public List<IPRecord> getIPHistory(UUID uuid) {
        String sql = "SELECT uuid, player_name, ip, timestamp FROM " + tablePrefix
                + "ip_history WHERE uuid=? ORDER BY timestamp DESC";
        return queryList(sql, stmt -> stmt.setString(1, uuid.toString()), this::mapIPRecord);
    }

    // ── Name History ─────────────────────────────────────────────────────────

    @Override
    public void recordNameChange(UUID uuid, String oldName, String newName) {
        String sql = "INSERT INTO " + tablePrefix + "name_history (uuid, old_name, new_name, timestamp) VALUES (?,?,?,?)";
        execUpdate(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, oldName);
            stmt.setString(3, newName);
            stmt.setLong(4, System.currentTimeMillis());
        });
    }

    @Override
    public List<NameRecord> getNameHistory(UUID uuid) {
        String sql = "SELECT old_name, new_name, timestamp FROM " + tablePrefix
                + "name_history WHERE uuid=? ORDER BY timestamp DESC";
        return queryList(sql, stmt -> stmt.setString(1, uuid.toString()), this::mapNameRecord);
    }

    // ── Punishment Counts ────────────────────────────────────────────────────

    @Override
    public int getTotalPunishments(PunishmentType type) {
        if (type == null) {
            return queryOne("SELECT COUNT(*) FROM " + tablePrefix + "punishments",
                    stmt -> {}, rs -> rs.getInt(1));
        }
        return queryOne("SELECT COUNT(*) FROM " + tablePrefix + "punishments WHERE type=?",
                stmt -> stmt.setString(1, type.name()), rs -> rs.getInt(1));
    }

    @Override
    public int getActivePunishmentCount(PunishmentType type) {
        String sql = "SELECT COUNT(*) FROM " + tablePrefix + "punishments"
                + " WHERE active=TRUE AND (date_end<=0 OR date_end>?)"
                + (type != null ? " AND type=?" : "");
        return queryOne(sql, stmt -> {
            stmt.setLong(1, System.currentTimeMillis());
            if (type != null) stmt.setString(2, type.name());
        }, rs -> rs.getInt(1));
    }

    @Override
    public List<Punishment> getAllPunishments() {
        return queryList("SELECT * FROM " + tablePrefix
                + "punishments ORDER BY id ASC",
                stmt -> {}, this::mapPunishment);
    }

    // ── Staff Rollback ───────────────────────────────────────────────────────

    @Override
    public int rollbackStaff(UUID executorUUID, long olderThan) {
        String sql = "UPDATE " + tablePrefix + "punishments"
                + " SET active=FALSE, removed_by_uuid=?, removed_by_name=?"
                + " WHERE executor_uuid=? AND date_start<? AND active=TRUE";
        return execUpdate(sql, stmt -> {
            stmt.setString(1, "00000000-0000-0000-0000-000000000000");
            stmt.setString(2, "Console");
            stmt.setString(3, executorUUID.toString());
            stmt.setLong(4, olderThan);
        });
    }

    @Override
    public int pruneHistory(UUID uuid, long olderThan) {
        String sql = "DELETE FROM " + tablePrefix + "punishments WHERE uuid=? AND date_start<?";
        return execUpdate(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, olderThan);
        });
    }

    // ── IP Ban Check ─────────────────────────────────────────────────────────

    @Override
    public boolean isIPBanned(String ip, String serverScope) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tablePrefix + "punishments"
                + " WHERE ip=? AND type='IPBAN' AND active=TRUE AND (date_end<=0 OR date_end>?)");
        List<Object> params = new ArrayList<>();
        params.add(ip);
        params.add(System.currentTimeMillis());
        appendScope(sql, params, serverScope);
        return queryOne(sql.toString(), stmt -> bindParams(stmt, params), rs -> rs.getInt(1) > 0);
    }

    // ── Staff Notes ──────────────────────────────────────────────────────────

    @Override
    public void saveNote(StaffNote note) {
        if (note.getId() > 0 && queryOne("SELECT COUNT(*) FROM " + tablePrefix + "staff_notes WHERE id=?",
                stmt -> stmt.setLong(1, note.getId()), rs -> rs.getInt(1) > 0)) {
            // Note exists — update it
            String sql = "UPDATE " + tablePrefix + "staff_notes SET target_uuid=?, target_name=?, note=?, "
                    + "author_uuid=?, author_name=?, timestamp=? WHERE id=?";
            execUpdate(sql, stmt -> {
                stmt.setString(1, note.getTargetUUID().toString());
                stmt.setString(2, note.getTargetName());
                stmt.setString(3, note.getNote());
                stmt.setString(4, note.getAuthorUUID().toString());
                stmt.setString(5, note.getAuthorName());
                stmt.setLong(6, note.getTimestamp());
                stmt.setLong(7, note.getId());
            });
            return;
        }
        if (note.getId() <= 0) {
            note.setId(nextNoteId.getAndIncrement());
        } else {
            long id = note.getId();
            if (id >= nextNoteId.get()) {
                nextNoteId.set(id + 1);
            }
        }
        String sql = "INSERT INTO " + tablePrefix + "staff_notes (id, target_uuid, target_name, note, "
                + "author_uuid, author_name, timestamp) VALUES (?,?,?,?,?,?,?)";
        execUpdate(sql, stmt -> {
            stmt.setLong(1, note.getId());
            stmt.setString(2, note.getTargetUUID().toString());
            stmt.setString(3, note.getTargetName());
            stmt.setString(4, note.getNote());
            stmt.setString(5, note.getAuthorUUID().toString());
            stmt.setString(6, note.getAuthorName());
            stmt.setLong(7, note.getTimestamp());
        });
    }

    @Override
    public void deleteNote(long id) {
        execUpdate("DELETE FROM " + tablePrefix + "staff_notes WHERE id=?",
                stmt -> stmt.setLong(1, id));
    }

    @Override
    public List<StaffNote> getNotes(UUID targetUUID) {
        String sql = "SELECT * FROM " + tablePrefix + "staff_notes WHERE target_uuid=? ORDER BY timestamp DESC";
        return queryList(sql, stmt -> stmt.setString(1, targetUUID.toString()), this::mapStaffNote);
    }

    @Override
    public List<StaffNote> getAllNotes() {
        String sql = "SELECT * FROM " + tablePrefix + "staff_notes ORDER BY timestamp DESC";
        return queryList(sql, stmt -> {}, this::mapStaffNote);
    }

    // ── Scope Helper ─────────────────────────────────────────────────────────

    private static void appendScope(StringBuilder sql, List<Object> params, String serverScope) {
        if (serverScope != null && !serverScope.isEmpty() && !serverScope.equals("*")) {
            sql.append(" AND (server_scope=? OR server_scope='*' OR server_scope IS NULL)");
            params.add(serverScope);
        }
    }

    // ── Row Mapping ──────────────────────────────────────────────────────────

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        return Punishment.builder()
                .id(rs.getLong("id"))
                .type(PunishmentType.valueOf(rs.getString("type")))
                .uuid(UUID.fromString(rs.getString("uuid")))
                .ip(rs.getString("ip"))
                .name(rs.getString("name"))
                .reason(rs.getString("reason"))
                .executorUUID(getUuid(rs, "executor_uuid"))
                .executorName(rs.getString("executor_name"))
                .removedByUUID(getUuid(rs, "removed_by_uuid"))
                .removedByName(rs.getString("removed_by_name"))
                .removalReason(rs.getString("removal_reason"))
                .dateStart(rs.getLong("date_start"))
                .dateEnd(rs.getLong("date_end"))
                .serverScope(rs.getString("server_scope"))
                .serverOrigin(rs.getString("server_origin"))
                .silent(rs.getBoolean("silent"))
                .ipPunish(rs.getBoolean("ip_punish"))
                .active(rs.getBoolean("active"))
                .templateName(rs.getString("template_name"))
                .duration(rs.getLong("duration"))
                .build();
    }

    private IPRecord mapIPRecord(ResultSet rs) throws SQLException {
        return new IPRecord(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("player_name"),
                rs.getString("ip"),
                rs.getLong("timestamp"));
    }

    private NameRecord mapNameRecord(ResultSet rs) throws SQLException {
        return new NameRecord(
                rs.getString("old_name"),
                rs.getString("new_name"),
                rs.getLong("timestamp"));
    }

    private StaffNote mapStaffNote(ResultSet rs) throws SQLException {
        return StaffNote.builder()
                .id(rs.getLong("id"))
                .targetUUID(UUID.fromString(rs.getString("target_uuid")))
                .targetName(rs.getString("target_name"))
                .note(rs.getString("note"))
                .authorUUID(UUID.fromString(rs.getString("author_uuid")))
                .authorName(rs.getString("author_name"))
                .timestamp(rs.getLong("timestamp"))
                .build();
    }

    // ── JDBC Utilities ───────────────────────────────────────────────────────

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private int execUpdate(String sql, StatementBinder binder) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            binder.bind(stmt);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "SQL execUpdate failed: " + sql, e);
            return 0;
        }
    }

    private int execUpdate(String sql) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "SQL execUpdate (raw) failed: " + sql, e);
            return 0;
        }
    }

    private <T> T queryOne(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            binder.bind(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapper.map(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "SQL queryOne failed: " + sql, e);
        }
        return null;
    }

    private <T> List<T> queryList(String sql, StatementBinder binder, RowMapper<T> mapper) {
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            binder.bind(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "SQL queryList failed: " + sql, e);
        }
        return results;
    }

    private static void bindParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof String) {
                stmt.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) param);
            } else if (param == null) {
                stmt.setNull(i + 1, java.sql.Types.NULL);
            } else {
                stmt.setString(i + 1, param.toString());
            }
        }
    }

    private static void setUuid(PreparedStatement stmt, int index, UUID uuid) throws SQLException {
        if (uuid != null) {
            stmt.setString(index, uuid.toString());
        } else {
            stmt.setNull(index, java.sql.Types.VARCHAR);
        }
    }

    private static UUID getUuid(ResultSet rs, String column) throws SQLException {
        String val = rs.getString(column);
        return (val != null && !val.isEmpty()) ? UUID.fromString(val) : null;
    }
}