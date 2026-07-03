package com.kyssta.casualbans.command;

import com.kyssta.casualbans.manager.ImportManager;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CasualBansCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    private ImportManager importManager;

    public CasualBansCommand() {
        super();
        this.importManager = new ImportManager(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.admin")) return true;

        if (args.length == 0) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.version", "$version",
                plugin.getDescription().getVersion()));
            MessageUtil.send(sender, "<gray>/casualbans reload</gray> <dark_gray>- Reload configuration</dark_gray>");
            MessageUtil.send(sender, "<gray>/casualbans status</gray> <dark_gray>- Show plugin health status</dark_gray>");
            MessageUtil.send(sender, "<gray>/casualbans info</gray> <dark_gray>- Show plugin info</dark_gray>");
            MessageUtil.send(sender, "<gray>/casualbans import</gray> <dark_gray>- Import from another plugin</dark_gray>");
            MessageUtil.send(sender, "<gray>/casualbans allow <add|remove|check> <user></gray> <dark_gray>- Allowlist management</dark_gray>");
            MessageUtil.send(sender, "<gray>/casualbans unlink <player></gray> <dark_gray>- Remove IP association</dark_gray>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.reload-start"));
                try {
                    plugin.reload();
                    MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.reload-done"));
                } catch (Exception e) {
                    MessageUtil.sendPrefix(sender, "<red>Reload failed: " + e.getMessage() + "</red>");
                    plugin.getLogger().warning("Reload failed for " + sender.getName() + ": " + e.getMessage());
                }
            }
            case "status" -> handleStatus(sender);
            case "info" -> {
                MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.version", "$version",
                    plugin.getDescription().getVersion()));
                MessageUtil.send(sender, MessageUtil.getMessage("casualbans.server", "$server",
                    plugin.getServerName()));
                MessageUtil.send(sender, MessageUtil.getMessage("casualbans.storage", "$type",
                    plugin.getConfig().getString("storage.driver", "JSON")));
                MessageUtil.send(sender, MessageUtil.getMessage("casualbans.connected", "$status",
                    String.valueOf(plugin.getStorageProvider().isConnected())));
                if (plugin.getSyncManager().isEnabled()) {
                    MessageUtil.send(sender, MessageUtil.getMessage("casualbans.sync", "$status", "Enabled"));
                }
            }
            case "import" -> handleImport(sender, args);
            case "allow" -> {
                if (args.length < 3) {
                    MessageUtil.sendPrefix(sender, MessageUtil.getMessage("invalid-usage", "$command", "casualbans allow <add|remove|check> <player>"));
                    return true;
                }
                // Basic allowlist functionality
                MessageUtil.send(sender, "<yellow>Allowlist feature coming soon.</yellow>");
            }
            case "unlink" -> {
                if (args.length < 2) {
                    MessageUtil.sendPrefix(sender, MessageUtil.getMessage("invalid-usage", "$command", "casualbans unlink <player>"));
                    return true;
                }
                var uuid = resolveUUID(args[1]);
                if (uuid == null) {
                    MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
                    return true;
                }
                MessageUtil.send(sender, "<yellow>Unlink feature coming soon.</yellow>");
            }
            default ->
                MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.unknown-cmd"));
        }
        return true;
    }

    /**
     * Shows a comprehensive health status summary for all plugin subsystems.
     */
    private void handleStatus(CommandSender sender) {
        MessageUtil.sendPrefix(sender, "<gold>=== CasualBans Status ===</gold>");
        MessageUtil.send(sender, "");

        // ── Plugin info ──
        MessageUtil.send(sender, "<gold>Plugin:</gold>");
        MessageUtil.send(sender, "  <gray>Version:</gray> <green>" + plugin.getDescription().getVersion() + "</green>");
        MessageUtil.send(sender, "  <gray>Server:</gray> <green>" + plugin.getServerName() + "</green>");

        // ── Storage health ──
        String driver = plugin.getConfig().getString("storage.driver", "JSON");
        boolean storageConnected;
        try {
            storageConnected = plugin.getStorageProvider() != null && plugin.getStorageProvider().isConnected();
        } catch (Exception e) {
            storageConnected = false;
        }
        String storageStatus = storageConnected ? "<green>Connected</green>" : "<red>Disconnected</red>";
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>Storage:</gold>");
        MessageUtil.send(sender, "  <gray>Driver:</gray> <green>" + driver + "</green>");
        MessageUtil.send(sender, "  <gray>Status:</gray> " + storageStatus);

        // ── Sync health ──
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>Cross-Server Sync:</gold>");
        boolean syncEnabled;
        try {
            syncEnabled = plugin.getSyncManager() != null && plugin.getSyncManager().isEnabled();
        } catch (Exception e) {
            syncEnabled = false;
        }
        String syncDetail;
        if (syncEnabled) {
            syncDetail = "<green>Enabled</green> <gray>(SQL-backed, polling every 2s)</gray>";
        } else if (driver.equalsIgnoreCase("JSON")) {
            syncDetail = "<yellow>Disabled</yellow> <gray>(requires SQL database for sync)</gray>";
        } else {
            syncDetail = "<yellow>Disabled</yellow> <gray>(sync.enabled=false in config.yml)</gray>";
        }
        MessageUtil.send(sender, "  " + syncDetail);

        // ── GeoIP health ──
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>GeoIP:</gold>");
        boolean geoipEnabled = plugin.getConfigManager().isGeoipEnabled();
        boolean geoipAvailable;
        try {
            geoipAvailable = plugin.getGeoIPManager() != null && plugin.getGeoIPManager().isAvailable();
        } catch (Exception e) {
            geoipAvailable = false;
        }
        if (!geoipEnabled) {
            MessageUtil.send(sender, "  <yellow>Disabled</yellow> <gray>(geoip.enabled=false in config.yml)</gray>");
        } else if (geoipAvailable) {
            MessageUtil.send(sender, "  <green>Enabled</green> <gray>(database loaded)</gray>");
        } else {
            MessageUtil.send(sender, "  <red>Database not found</red>");
            MessageUtil.send(sender, "  <gray>Place GeoLite2-Country.mmdb in the plugin folder.</gray>");
        }

        // ── Webhook health ──
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>Webhooks:</gold>");
        boolean webhooksEnabled;
        try {
            webhooksEnabled = plugin.getWebhookManager() != null && plugin.getWebhookManager().isEnabled();
        } catch (Exception e) {
            webhooksEnabled = false;
        }
        if (!webhooksEnabled) {
            MessageUtil.send(sender, "  <yellow>Disabled</yellow> <gray>(webhooks.enabled=false in config.yml)</gray>");
        } else {
            String punUrl = plugin.getConfig().getString("webhooks.punishment-url", "");
            String altUrl = plugin.getConfig().getString("webhooks.alt-url", "");
            String staffUrl = plugin.getConfig().getString("webhooks.staff-url", "");
            boolean hasPun = punUrl != null && !punUrl.isEmpty();
            boolean hasAlt = altUrl != null && !altUrl.isEmpty();
            boolean hasStaff = staffUrl != null && !staffUrl.isEmpty();
            MessageUtil.send(sender, "  <green>Enabled</green>");
            MessageUtil.send(sender, "  <gray>Punishment URL:</gray> " + (hasPun ? "<green>Configured</green>" : "<yellow>Not set</yellow>"));
            MessageUtil.send(sender, "  <gray>Alt Detection URL:</gray> " + (hasAlt ? "<green>Configured</green>" : "<yellow>Not set</yellow>"));
            MessageUtil.send(sender, "  <gray>Staff URL:</gray> " + (hasStaff ? "<green>Configured</green>" : "<yellow>Not set</yellow>"));
        }

        // ── Lockdown health ──
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>Lockdown:</gold>");
        boolean lockedDown;
        try {
            lockedDown = plugin.getLockdownManager() != null && plugin.getLockdownManager().isLockedDown();
        } catch (Exception e) {
            lockedDown = false;
        }
        if (lockedDown) {
            String reason = plugin.getLockdownManager().getReason();
            MessageUtil.send(sender, "  <red>Active</red>"
                + (reason != null ? " <gray>(Reason: " + reason + ")</gray>" : ""));
        } else {
            MessageUtil.send(sender, "  <green>Inactive</green>");
        }

        // ── Web interface ──
        boolean webEnabled = plugin.getConfig().getBoolean("web.enabled", false);
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>Web Interface:</gold>");
        if (webEnabled) {
            int port = plugin.getConfig().getInt("web.port", 8291);
            MessageUtil.send(sender, "  <green>Enabled</green> <gray>(port " + port + ")</gray>");
        } else {
            MessageUtil.send(sender, "  <yellow>Disabled</yellow> <gray>(web.enabled=false in config.yml)</gray>");
        }

        // ── Performance ──
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<gold>Thread Pools:</gold>");
        MessageUtil.send(sender, "  <gray>Async threads:</gray> <green>"
            + plugin.getConfig().getInt("performance.async-threads", 4) + "</green>");
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<dark_gray>Use /casualbans info for detailed plugin information.</dark_gray>");
    }

    /**
     * Handles the /casualbans import subcommand.
     * <p>
     * Usage:
     *   /casualbans import litebans &lt;host&gt; &lt;db&gt; &lt;user&gt; &lt;pass&gt; [port]
     *   /casualbans import advancedban &lt;host&gt; &lt;db&gt; &lt;user&gt; &lt;pass&gt; [port]
     *   /casualbans import vanilla
     *   /casualbans import json &lt;file&gt;
     */
    private void handleImport(CommandSender sender, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("console-only"));
            return;
        }

        if (args.length < 2) {
            sendImportUsage(sender);
            return;
        }

        String subSource = args[1].toLowerCase();

        switch (subSource) {
            case "litebans" -> startLiteBansImport(sender, args);
            case "advancedban" -> startAdvancedBanImport(sender, args);
            case "vanilla" -> startVanillaImport(sender);
            case "json" -> startJsonImport(sender, args);
            default -> sendImportUsage(sender);
        }
    }

    private void startLiteBansImport(CommandSender sender, String[] args) {
        if (args.length < 6) {
            MessageUtil.send(sender, "<yellow>Usage: /casualbans import litebans <host> <database> <username> <password> [port]</yellow>");
            MessageUtil.send(sender, "<gray>Example: /casualbans import litebans localhost litebans root mypass 3306</gray>");
            return;
        }

        String host = args[2];
        String database = args[3];
        String username = args[4];
        String password = args[5];
        int port;
        try {
            port = args.length > 6 ? Integer.parseInt(args[6]) : 3306;
        } catch (NumberFormatException e) {
            MessageUtil.sendPrefix(sender, "<red>Invalid port number: '" + args[6] + "'. Using default 3306.</red>");
            port = 3306;
        }

        ImportManager.SqlConfig config = new ImportManager.SqlConfig(host, port, database, username, password);

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.import-start",
            "$source", "LiteBans",
            "$host", host,
            "$port", String.valueOf(port),
            "$database", database));

        MessageUtil.send(sender, "<gray>Import is running in the background. Progress will appear below.</gray>");

        try {
            importManager.startImport(
                ImportManager.ImportSource.liteBans(config),
                progress -> {
                    for (String line : progress.toSummary().split("\\n")) {
                        MessageUtil.send(sender, line);
                    }
                    if (progress.error()) {
                        MessageUtil.sendPrefix(sender, "<red>Import error: " + progress.errorMessage() + "</red>");
                    }
                }
            );
        } catch (Exception e) {
            MessageUtil.sendPrefix(sender, "<red>Failed to start LiteBans import: " + e.getMessage() + "</red>");
            plugin.getLogger().warning("LiteBans import start failed: " + e.getMessage());
        }
    }

    private void startAdvancedBanImport(CommandSender sender, String[] args) {
        if (args.length < 6) {
            MessageUtil.send(sender, "<yellow>Usage: /casualbans import advancedban <host> <database> <username> <password> [port]</yellow>");
            MessageUtil.send(sender, "<gray>Example: /casualbans import advancedban localhost advancedban root mypass 3306</gray>");
            return;
        }

        String host = args[2];
        String database = args[3];
        String username = args[4];
        String password = args[5];
        int port;
        try {
            port = args.length > 6 ? Integer.parseInt(args[6]) : 3306;
        } catch (NumberFormatException e) {
            MessageUtil.sendPrefix(sender, "<red>Invalid port number: '" + args[6] + "'. Using default 3306.</red>");
            port = 3306;
        }

        ImportManager.SqlConfig config = new ImportManager.SqlConfig(host, port, database, username, password);

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.import-start",
            "$source", "AdvancedBan",
            "$host", host,
            "$port", String.valueOf(port),
            "$database", database));

        MessageUtil.send(sender, "<gray>Import is running in the background. Progress will appear below.</gray>");

        try {
            importManager.startImport(
                ImportManager.ImportSource.advancedBan(config),
                progress -> {
                    for (String line : progress.toSummary().split("\\n")) {
                        MessageUtil.send(sender, line);
                    }
                    if (progress.error()) {
                        MessageUtil.sendPrefix(sender, "<red>Import error: " + progress.errorMessage() + "</red>");
                    }
                }
            );
        } catch (Exception e) {
            MessageUtil.sendPrefix(sender, "<red>Failed to start AdvancedBan import: " + e.getMessage() + "</red>");
            plugin.getLogger().warning("AdvancedBan import start failed: " + e.getMessage());
        }
    }

    private void startVanillaImport(CommandSender sender) {
        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.import-start",
            "$source", "vanilla Minecraft",
            "$host", "",
            "$port", "",
            "$database", ""));

        MessageUtil.send(sender, "<gray>Importing from banned-players.json and banned-ips.json in server root.</gray>");

        try {
            importManager.startImport(
                ImportManager.ImportSource.vanilla(),
                progress -> {
                    for (String line : progress.toSummary().split("\\n")) {
                        MessageUtil.send(sender, line);
                    }
                    if (progress.error()) {
                        MessageUtil.sendPrefix(sender, "<red>Import error: " + progress.errorMessage() + "</red>");
                    }
                }
            );
        } catch (Exception e) {
            MessageUtil.sendPrefix(sender, "<red>Failed to start vanilla import: " + e.getMessage() + "</red>");
            plugin.getLogger().warning("Vanilla import start failed: " + e.getMessage());
        }
    }

    private void startJsonImport(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "<yellow>Usage: /casualbans import json <file></yellow>");
            MessageUtil.send(sender, "<gray>  File can be an absolute path or a path relative to the server directory.</gray>");
            MessageUtil.send(sender, "<gray>  Example: /casualbans import json /path/to/export.json</gray>");
            MessageUtil.send(sender, "<gray>  Example: /casualbans import json export.json</gray>");
            return;
        }

        String filePath = args[2];
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(".", filePath);
        }

        if (!file.exists()) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.import-fail", "$error",
                "File not found: " + file.getAbsolutePath()));
            MessageUtil.send(sender, "<gray>Make sure the file path is correct and the file exists.</gray>");
            return;
        }

        if (!file.canRead()) {
            MessageUtil.sendPrefix(sender, "<red>Cannot read file: " + file.getAbsolutePath()
                + ". Check file permissions.</red>");
            return;
        }

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("casualbans.import-start",
            "$source", "JSON",
            "$host", file.getAbsolutePath(),
            "$port", "",
            "$database", ""));

        try {
            importManager.startImport(
                ImportManager.ImportSource.json(file),
                progress -> {
                    for (String line : progress.toSummary().split("\\n")) {
                        MessageUtil.send(sender, line);
                    }
                    if (progress.error()) {
                        MessageUtil.sendPrefix(sender, "<red>Import error: " + progress.errorMessage() + "</red>");
                    }
                }
            );
        } catch (Exception e) {
            MessageUtil.sendPrefix(sender, "<red>Failed to start JSON import: " + e.getMessage() + "</red>");
            plugin.getLogger().warning("JSON import start failed: " + e.getMessage());
        }
    }

    private void sendImportUsage(CommandSender sender) {
        MessageUtil.sendPrefix(sender, "<gold>Import Usage:</gold>");
        MessageUtil.send(sender, "  <gray>/casualbans import litebans &lt;host&gt; &lt;database&gt; &lt;user&gt; &lt;pass&gt; [port]</gray>");
        MessageUtil.send(sender, "  <gray>/casualbans import advancedban &lt;host&gt; &lt;database&gt; &lt;user&gt; &lt;pass&gt; [port]</gray>");
        MessageUtil.send(sender, "  <gray>/casualbans import vanilla</gray>");
        MessageUtil.send(sender, "  <gray>/casualbans import json &lt;file&gt;</gray>");
        MessageUtil.send(sender, "");
        MessageUtil.send(sender, "<dark_gray>Note: Import must be run from console. See example-import.json in the plugin data directory for the JSON format.</dark_gray>");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("reload", "status", "info", "import", "backup", "restore", "export", "allow", "unlink")
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            File backupsDir = new File(plugin.getDataFolder(), "backups");
            if (backupsDir.isDirectory()) {
                String[] files = backupsDir.list((dir, name) -> name.endsWith(".zip"));
                if (files != null) {
                    return Stream.of(files)
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
            return new ArrayList<>();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return Stream.of("litebans", "advancedban", "vanilla", "json")
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private static int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
