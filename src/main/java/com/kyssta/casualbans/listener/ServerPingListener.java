package com.kyssta.casualbans.listener;

import com.kyssta.casualbans.CasualBans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class ServerPingListener implements Listener {

    private final CasualBans plugin;

    public ServerPingListener(CasualBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        try {
            // Optionally show player count info on the server ping
            // (Could be extended to show ban stats, etc.)
        } catch (Exception e) {
            plugin.getLogger().warning("Unhandled error in ServerListPingEvent: " + e.getMessage());
        }
    }
}
