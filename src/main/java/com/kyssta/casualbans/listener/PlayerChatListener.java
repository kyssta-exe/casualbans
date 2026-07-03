package com.kyssta.casualbans.listener;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final CasualBans plugin;

    public PlayerChatListener(CasualBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        try {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            // Check mutechat (server-wide chat disabled)
            if (plugin.getChatManager().isMuteChatEnabled()) {
                if (!player.hasPermission("casualbans.mutechat.bypass")) {
                    event.setCancelled(true);
                    MessageUtil.send(player, plugin.getConfigManager().getMessagesConfig().getString("mutechat.blocked",
                        "<red>Chat is currently disabled.</red>"));
                    return;
                }
            }

            // Check togglechat (player toggled own chat off)
            if (plugin.getChatManager().isChatToggledOff(uuid)) {
                event.setCancelled(true);
                MessageUtil.send(player, "<red>Your chat is toggled off. Use /togglechat to re-enable.</red>");
                return;
            }

            // Check if player is muted
            plugin.getAsyncExecutor().submit(() -> {
                try {
                    boolean muted = plugin.getPunishmentManager().isMuted(uuid, "*");
                    if (muted) {
                        // Cancel on main thread
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            try {
                                event.setCancelled(true);
                                var mute = plugin.getPunishmentManager().getActiveMute(uuid, "*");
                                if (mute != null) {
                                    String msg = mute.isPermanent()
                                        ? plugin.getConfigManager().getMessagesConfig().getString("mute.message-permanent",
                                            "<red>You are permanently muted! Reason: $reason</red>")
                                            .replace("$reason", mute.getReason())
                                        : plugin.getConfigManager().getMessagesConfig().getString("mute.message",
                                            "<red>You are muted! ($duration remaining)</red><newline/><red>Reason:</red> $reason")
                                            .replace("$reason", mute.getReason())
                                            .replace("$duration", mute.getDurationString());
                                    MessageUtil.send(player, msg);
                                }

                                // Notify staff
                                boolean notifyMuted = plugin.getConfig().getBoolean("notifications.muted-player-chat", true);
                                if (notifyMuted) {
                                    String notify = "<gray>" + player.getName() + "</gray> <dark_gray>tried to speak but is muted.</dark_gray>";
                                    plugin.getServer().getOnlinePlayers().stream()
                                        .filter(p -> p.hasPermission("casualbans.notify"))
                                        .forEach(p -> MessageUtil.send(p, notify));
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error processing mute cancel for " + player.getName() + ": " + e.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking mute for " + player.getName() + ": " + e.getMessage());
                }
            });

            // Delay cancellation check to avoid conflict with the async check
            // We handle it synchronously here with a cache
            try {
                var mute = plugin.getPunishmentManager().getCachedMute(uuid);
                if (mute != null) {
                    event.setCancelled(true);
                    String msg = mute.isPermanent()
                        ? "<red>You are permanently muted! Reason: " + mute.getReason() + "</red>"
                        : "<red>You are muted! (" + mute.getDurationString() + " remaining)</red><newline/><red>Reason:</red> " + mute.getReason();
                    MessageUtil.send(player, msg);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking cached mute for " + player.getName() + ": " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Unhandled error in PlayerChatEvent for "
                + (event.getPlayer() != null ? event.getPlayer().getName() : "unknown")
                + ": " + e.getMessage());
        }
    }
}
