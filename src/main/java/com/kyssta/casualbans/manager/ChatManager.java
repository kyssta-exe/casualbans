package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks server-wide mutechat state and per-player togglechat state.
 */
public class ChatManager {

    private boolean muteChatEnabled;
    private final Map<UUID, Boolean> toggledChat = new ConcurrentHashMap<>();

    private final CasualBans plugin;

    public ChatManager(CasualBans plugin) {
        this.plugin = plugin;
        this.muteChatEnabled = false;
    }

    // ── MuteChat (server-wide) ──

    /**
     * Returns whether server-wide chat is currently disabled via /mutechat.
     */
    public boolean isMuteChatEnabled() {
        return muteChatEnabled;
    }

    /**
     * Enable or disable the server-wide mutechat.
     */
    public void setMuteChatEnabled(boolean enabled) {
        this.muteChatEnabled = enabled;
    }

    // ── ToggleChat (per-player) ──

    /**
     * Check if a specific player has their chat toggled off via /togglechat.
     */
    public boolean isChatToggledOff(UUID uuid) {
        return toggledChat.getOrDefault(uuid, false);
    }

    /**
     * Set whether a specific player has their chat toggled off.
     */
    public void setChatToggled(UUID uuid, boolean toggledOff) {
        if (toggledOff) {
            toggledChat.put(uuid, true);
        } else {
            toggledChat.remove(uuid);
        }
    }

    /**
     * Remove a player's toggle state (e.g., on quit).
     */
    public void removePlayer(UUID uuid) {
        toggledChat.remove(uuid);
    }

    /**
     * Clear all toggled states (e.g., on reload).
     */
    public void clear() {
        toggledChat.clear();
        muteChatEnabled = false;
    }
}
