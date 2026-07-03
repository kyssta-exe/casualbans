package com.kyssta.casualbans.listener;

import com.kyssta.casualbans.CasualBans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final CasualBans plugin;

    public PlayerQuitListener(CasualBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            // Clean up togglechat state
            if (event.getPlayer() != null) {
                plugin.getChatManager().removePlayer(event.getPlayer().getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Unhandled error in PlayerQuitEvent: " + e.getMessage());
        }
    }
}
