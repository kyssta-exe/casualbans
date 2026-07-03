package com.kyssta.casualbans.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kyssta.casualbans.CasualBans;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Tracks and persists the server lockdown state.
 * Lockdown prevents all non-exempt players from joining.
 */
public class LockdownManager {

    private final CasualBans plugin;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean lockedDown;
    private String reason;

    public LockdownManager(CasualBans plugin) {
        this.plugin = plugin;
        this.lockedDown = false;
        this.reason = null;
    }

    /**
     * Restore lockdown state from disk (called during onEnable).
     */
    public void load() {
        if (!plugin.getConfigManager().isLockdownPersistsAcrossRestart()) {
            return;
        }

        File file = getLockdownFile();
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            LockdownData data = GSON.fromJson(reader, LockdownData.class);
            if (data != null) {
                this.lockedDown = data.lockedDown;
                this.reason = data.reason;
                if (lockedDown) {
                    plugin.getLogger().info("Lockdown restored: " + (reason != null ? reason : "No reason"));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load lockdown state: " + e.getMessage());
        }
    }

    /**
     * Check if the server is currently locked down.
     */
    public boolean isLockedDown() {
        return lockedDown;
    }

    /**
     * Activate lockdown with an optional reason.
     */
    public void setLockdown(String reason) {
        this.lockedDown = true;
        this.reason = reason;
        save();
    }

    /**
     * End the active lockdown.
     */
    public void endLockdown() {
        this.lockedDown = false;
        this.reason = null;
        save();
    }

    /**
     * Get the current lockdown reason (may be null).
     */
    public String getReason() {
        return reason;
    }

    /**
     * Save the current lockdown state to disk.
     */
    private void save() {
        File file = getLockdownFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        LockdownData data = new LockdownData(lockedDown, reason);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save lockdown state: " + e.getMessage());
        }
    }

    private File getLockdownFile() {
        return new File(plugin.getDataFolder(), "data/lockdown.json");
    }

    /**
     * JSON-serialisable lockdown data.
     */
    private record LockdownData(boolean lockedDown, String reason) {}
}
