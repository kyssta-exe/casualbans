package com.kyssta.casualbans.listener;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.UUIDUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final CasualBans plugin;

    public PlayerJoinListener(CasualBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            var player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
            String name = player.getName();

            // Cache UUID/name mapping
            UUIDUtil.cacheName(uuid, name);

            // Record IP address for history
            plugin.getAsyncExecutor().submit(() -> {
                try {
                    plugin.getStorageProvider().recordIPAddress(uuid, ip, name);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to record IP address for " + name + ": " + e.getMessage());
                }
            });

            // Check for warnings on join
            plugin.getAsyncExecutor().submit(() -> {
                try {
                    var warnings = plugin.getPunishmentManager().getActiveWarnings(uuid);
                    if (!warnings.isEmpty()) {
                        MessageUtil.send(player, "<gold>You have <red>" + warnings.size() + "</red> active warning(s).</gold>");
                        for (var w : warnings) {
                            MessageUtil.send(player, "<gray>-</gray> <green>" + w.getReason() + "</green> <gray>(expires " + w.getDurationString() + ")</gray>");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to check warnings for " + name + ": " + e.getMessage());
                }
            });

            // DupeIP notification on join
            if (plugin.getConfig().getBoolean("notifications.dupeip-on-join", true)) {
                plugin.getAsyncExecutor().submit(() -> {
                    try {
                        List<UUID> accounts = plugin.getStorageProvider().getAccountsByIP(ip);
                        if (accounts.size() > 1) {
                            int threshold = plugin.getConfig().getInt("notifications.dupeip-on-join-threshold", 999999);
                            boolean checkBanned = plugin.getConfig().getBoolean("notifications.dupeip-on-banned-account", true);
                            boolean checkMuted = plugin.getConfig().getBoolean("notifications.dupeip-on-muted-account", false);

                            if (accounts.size() >= threshold) {
                                // Notify staff
                                StringBuilder msg = new StringBuilder("<gold>").append(name)
                                    .append("</gold> <gray>has</gray> <green>").append(accounts.size())
                                    .append("</green> <gray>accounts on</gray> <green>").append(ip).append("</green>");

                                // Check for banned accounts
                                if (checkBanned) {
                                    for (UUID altUUID : accounts) {
                                        if (!altUUID.equals(uuid) && plugin.getPunishmentManager().isBanned(altUUID, "*")) {
                                            msg.append("\n<red>  ⚠ Has banned accounts on this IP!</red>");
                                            break;
                                        }
                                    }
                                }

                                String finalMsg = msg.toString();
                                plugin.getServer().getOnlinePlayers().stream()
                                    .filter(p -> p.hasPermission("casualbans.notify"))
                                    .forEach(p -> MessageUtil.send(p, finalMsg));

                                // Discord webhook for alts
                                if (plugin.getWebhookManager().isEnabled() && !plugin.getWebhookManager().getAltUrl().isEmpty()) {
                                    java.util.List<com.kyssta.casualbans.manager.InvestigationManager.AccountInfo> altList = new java.util.ArrayList<>();
                                    for (UUID altUUID : accounts) {
                                        if (!altUUID.equals(uuid)) {
                                            String altName = com.kyssta.casualbans.util.UUIDUtil.getName(altUUID);
                                            boolean altBanned = plugin.getPunishmentManager().isBanned(altUUID, "*");
                                            boolean altMuted = plugin.getPunishmentManager().isMuted(altUUID, "*");
                                            boolean altOnline = org.bukkit.Bukkit.getPlayer(altUUID) != null;
                                            altList.add(new com.kyssta.casualbans.manager.InvestigationManager.AccountInfo(altUUID, altName, altBanned, altMuted, altOnline));
                                        }
                                    }
                                    if (!altList.isEmpty()) {
                                        plugin.getWebhookManager().sendAltWebhook(name, ip, altList);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to check duplicate IPs for " + name + ": " + e.getMessage());
                    }
                });
            }

            // Notify if player was previously banned (recently unbanned)
            plugin.getAsyncExecutor().submit(() -> {
                try {
                    var history = plugin.getStorageProvider().getHistory(uuid, 1);
                    if (!history.isEmpty()) {
                        var last = history.get(0);
                        if (last.getType().isBan() && !last.isActive()) {
                            // Previously banned but now unbanned - optional notification
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to check ban history for " + name + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Unhandled error in PlayerJoinEvent for "
                + (event.getPlayer() != null ? event.getPlayer().getName() : "unknown")
                + ": " + e.getMessage());
        }
    }
}
