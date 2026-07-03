package com.kyssta.casualbans.command;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.admin")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/casualbans <backup|restore|export|status>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "backup" -> handleBackup(sender);
            case "restore" -> handleRestore(sender, args);
            case "export" -> handleExport(sender);
            case "status" -> handleStatus(sender);
            default -> MessageUtil.sendError(sender, "Unknown subcommand. Use: backup, restore <file>, export, status");
        }
        return true;
    }

    private void handleBackup(CommandSender sender) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            MessageUtil.sendError(sender, "No data directory found.");
            return;
        }

        File backupDir = new File(plugin.getDataFolder(), "backups");
        backupDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupDir, "backup-" + timestamp + ".zip");

        plugin.getAsyncExecutor().execute(() -> {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
                zipDir(dataDir, dataDir, zos);
                String msg = MessageUtil.getMessage("backup.created", "$file", backupFile.getName());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.sendSuccess(sender, msg));
            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.sendError(sender, "Backup failed: " + e.getMessage()));
            }
        });
    }

    private void handleRestore(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "/casualbans restore <filename>");
            MessageUtil.send(sender, "<gray>Available backups:</gray>");
            File backupDir = new File(plugin.getDataFolder(), "backups");
            File[] files = backupDir.listFiles((d, name) -> name.endsWith(".zip"));
            if (files != null) {
                for (File f : files) {
                    MessageUtil.send(sender, "<dark_gray>  - " + f.getName() + "</dark_gray>");
                }
            }
            return;
        }

        File backupDir = new File(plugin.getDataFolder(), "backups");
        File backupFile = new File(backupDir, args[1]);
        if (!backupFile.exists()) {
            MessageUtil.sendError(sender, "Backup file not found: " + args[1]);
            return;
        }

        File dataDir = new File(plugin.getDataFolder(), "data");
        plugin.getAsyncExecutor().execute(() -> {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = new File(dataDir, entry.getName());
                    outFile.getParentFile().mkdirs();
                    if (!entry.isDirectory()) {
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = zis.read(buf)) > 0) fos.write(buf);
                        }
                    }
                    zis.closeEntry();
                }
                String msg = MessageUtil.getMessage("backup.restored", "$file", backupFile.getName());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtil.sendSuccess(sender, msg);
                    MessageUtil.send(sender, "<yellow>Reloading plugin to load restored data...</yellow>");
                    plugin.reload();
                });
            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.sendError(sender, "Restore failed: " + e.getMessage()));
            }
        });
    }

    private void handleExport(CommandSender sender) {
        File exportDir = new File(plugin.getDataFolder(), "exports");
        exportDir.mkdirs();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File exportFile = new File(exportDir, "export-" + timestamp + ".json");

        plugin.getAsyncExecutor().execute(() -> {
            try {
                var punishments = plugin.getStorageProvider().getAllActivePunishments(
                    PunishmentType.BAN, "*");
                var mutes = plugin.getStorageProvider().getAllActivePunishments(
                    PunishmentType.MUTE, "*");
                punishments.addAll(mutes);

                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                try (FileWriter w = new FileWriter(exportFile)) {
                    gson.toJson(punishments, w);
                }

                String msg = MessageUtil.getMessage("backup.exported", "$file", exportFile.getName());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.sendSuccess(sender, msg));
            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    MessageUtil.sendError(sender, "Export failed: " + e.getMessage()));
            }
        });
    }

    private void handleStatus(CommandSender sender) {
        MessageUtil.sendPrefix(sender, "<green>CasualBans v" + plugin.getDescription().getVersion()
            + " — Status</green>");

        var sp = plugin.getStorageProvider();
        MessageUtil.send(sender, "<gray>Storage:</gray> <green>"
            + plugin.getConfig().getString("storage.driver", "JSON") + "</green>"
            + " <dark_gray>(" + (sp.isConnected() ? "<green>Connected</green>" : "<red>Disconnected</red>") + ")</dark_gray>");

        MessageUtil.send(sender, "<gray>Active Bans:</gray> <green>"
            + sp.getActivePunishmentCount(PunishmentType.BAN) + "</green>");
        MessageUtil.send(sender, "<gray>Active Mutes:</gray> <green>"
            + sp.getActivePunishmentCount(PunishmentType.MUTE) + "</green>");
        MessageUtil.send(sender, "<gray>Active Warnings:</gray> <green>"
            + sp.getActivePunishmentCount(PunishmentType.WARN) + "</green>");
        MessageUtil.send(sender, "<gray>Total Punishments:</gray> <green>"
            + sp.getTotalPunishments(PunishmentType.BAN) + "</green>");

        MessageUtil.send(sender, "<gray>Sync:</gray> "
            + (plugin.getSyncManager().isEnabled() ? "<green>Enabled</green>" : "<dark_gray>Disabled</dark_gray>"));

        if (plugin.getWebhookManager().isEnabled())
            MessageUtil.send(sender, "<gray>Webhooks:</gray> <green>Enabled</green>");

        MessageUtil.send(sender, "<gray>Server:</gray> <green>" + plugin.getServerName() + "</green>");
    }

    private void zipDir(File baseDir, File dir, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                zipDir(baseDir, f, zos);
            } else {
                String entryName = baseDir.toPath().relativize(f.toPath()).toString();
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = fis.read(buf)) > 0) zos.write(buf);
                }
                zos.closeEntry();
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("backup", "restore", "export", "status");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            File[] files = backupDir.listFiles((d, n) -> n.endsWith(".zip"));
            if (files != null) {
                return Arrays.stream(files).map(File::getName).toList();
            }
        }
        return new ArrayList<>();
    }
}
