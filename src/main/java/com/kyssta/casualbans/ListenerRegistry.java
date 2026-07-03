package com.kyssta.casualbans;

import com.kyssta.casualbans.listener.*;
import org.bukkit.plugin.PluginManager;

/**
 * Registers all event listeners.
 */
public final class ListenerRegistry {

    private ListenerRegistry() {}

    public static void registerListeners(CasualBans plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(plugin), plugin);
        pm.registerEvents(new PlayerChatListener(plugin), plugin);
        pm.registerEvents(new PlayerLoginListener(plugin), plugin);
        pm.registerEvents(new PlayerQuitListener(plugin), plugin);
        pm.registerEvents(new ServerPingListener(plugin), plugin);
    }
}
