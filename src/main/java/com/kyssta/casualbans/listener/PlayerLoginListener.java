package com.kyssta.casualbans.listener;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class PlayerLoginListener implements Listener {

    private final CasualBans plugin;

    public PlayerLoginListener(CasualBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        try {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            String ip = event.getAddress() != null ? event.getAddress().getHostAddress() : "unknown";
            String serverScope = plugin.getDefaultServerScope();

            // Check lockdown
            if (plugin.getLockdownManager().isLockedDown()) {
                if (!player.hasPermission("casualbans.lockdown.bypass") && !player.isOp()) {
                    String reason = plugin.getLockdownManager().getReason();
                    String kickMsg = plugin.getConfigManager().getMessagesConfig().getString("lockdown.kick-message",
                        "<red>Server lockdown is active. Try again later.</red><newline/><gray>Reason:</gray> <green>$reason</green>")
                        .replace("$reason", reason != null ? reason : "Maintenance");
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, MessageUtil.legacy(kickMsg));
                    return;
                }
            }

            // Check GeoIP
            if (plugin.getConfigManager().isGeoipEnabled()) {
                String country = plugin.getGeoIPManager().getCountry(ip);
                if (country != null) {
                    if (plugin.getGeoIPManager().isBlocked(country)) {
                        String msg = plugin.getConfigManager().getMessagesConfig().getString("banned_message_geoip",
                            "<red>Your location is blacklisted: $geoip</red>").replace("$geoip", country);
                        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, MessageUtil.legacy(msg));
                        return;
                    }
                }
            }

            // Check if player is banned
            plugin.getAsyncExecutor().submit(() -> {
                try {
                    boolean banned = plugin.getPunishmentManager().isBanned(uuid, serverScope);
                    if (banned) {
                        var ban = plugin.getPunishmentManager().getActiveBan(uuid, serverScope);
                        if (ban != null) {
                            String kickMsg;
                            if (ban.isPermanent()) {
                                kickMsg = plugin.getConfigManager().getMessagesConfig().getString("ban.kick-message-permanent",
                                    "<red>You are permanently banned from this server.</red>\n<gray>Banned by:</gray> <green>$executor</green>\n<gray>Reason:</gray> <green>$reason</green>");
                            } else {
                                kickMsg = plugin.getConfigManager().getMessagesConfig().getString("ban.kick-message",
                                    "<red>You are banned from this server.</red>\n<gray>Banned by:</gray> <green>$executor</green>\n<gray>Reason:</gray> <green>$reason</green>\n<gray>Expires:</gray> <green>$duration</green>");
                            }
                            kickMsg = kickMsg
                                .replace("$executor", ban.getExecutorName())
                                .replace("$reason", ban.getReason())
                                .replace("$duration", ban.getDurationString());

                            String finalKickMsg = kickMsg;
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MessageUtil.legacy(finalKickMsg));
                            });
                            return;
                        }
                    }

                    // Also check IP bans
                    boolean ipBanned = plugin.getPunishmentManager().isIPBanned(ip, serverScope);
                    if (ipBanned) {
                        String kickMsg = "<red>Your IP address is banned from this server.</red>";
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, MessageUtil.legacy(kickMsg));
                        });
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking ban for " + player.getName() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Unhandled error in PlayerLoginEvent for "
                + (event.getPlayer() != null ? event.getPlayer().getName() : "unknown")
                + ": " + e.getMessage());
        }
    }
}
