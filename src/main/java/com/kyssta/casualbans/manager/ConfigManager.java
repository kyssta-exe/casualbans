package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

/**
 * Manages all configuration files for CasualBans.
 */
public class ConfigManager {

    private final CasualBans plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(CasualBans plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void reload() {
        plugin.reloadConfig();
        loadMessages();
    }

    private void loadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load messages into MessageUtil
        if (messagesConfig != null) {
            MessageUtil.loadMessages(messagesConfig.getValues(true));
        }
    }

    /**
     * Get a message string from messages.yml with variable replacement.
     */
    public String getMessage(String path, String... replacements) {
        String msg = messagesConfig.getString(path);
        if (msg == null || msg.isEmpty()) return "";

        for (int i = 0; i < replacements.length - 1; i += 2) {
            if (replacements[i] != null && replacements[i + 1] != null) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }
        return msg;
    }

    /**
     * Get message config section values as a flat map.
     */
    public Map<String, Object> getMessagesMap() {
        return messagesConfig.getValues(true);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    // ── Config helper methods used by managers ──

    public boolean isLockdownPersistsAcrossRestart() {
        return plugin.getConfig().getBoolean("lockdown.persists-across-restart", true);
    }

    public boolean isGeoipEnabled() {
        return plugin.getConfig().getBoolean("geoip.enabled", false);
    }

    public java.util.List<String> getGeoipBlacklist() {
        return plugin.getConfig().getStringList("geoip.blacklist");
    }

    public java.util.List<String> getGeoipWhitelist() {
        return plugin.getConfig().getStringList("geoip.whitelist");
    }

    public int getDefaultHistoryLimit() {
        return plugin.getConfig().getInt("history.default-limit", 10);
    }

    public boolean isMutesEnabled() {
        return plugin.getConfig().getBoolean("mutes.enabled", true);
    }

    public java.util.List<String> getMuteCommandBlacklist() {
        return plugin.getConfig().getStringList("mutes.command-blacklist");
    }

    public java.util.Map<String, Object> getDurationLimits() {
        return plugin.getConfig().getConfigurationSection("durations.limits").getValues(false);
    }

    public int getBansPerPage() {
        return plugin.getConfig().getInt("display.bans-per-page", 10);
    }

    public long getWarnExpireAfter() {
        String expire = plugin.getConfig().getString("warnings.expire-after", "30d");
        return com.kyssta.casualbans.util.TimeUtil.parseDuration(expire);
    }

    public boolean isSyncEnabled() {
        return plugin.getConfig().getBoolean("sync.enabled", true);
    }

    public boolean isDupeipSecurityEnabled() {
        return plugin.getConfig().getBoolean("security.dupeip", true);
    }

    public boolean isIpHistorySecurityEnabled() {
        return plugin.getConfig().getBoolean("security.iphistory", true);
    }

    public boolean isStaffRollbackSecurityEnabled() {
        return plugin.getConfig().getBoolean("security.staffrollback", true);
    }

    public java.util.List<String> getWhitelist() {
        return plugin.getConfig().getStringList("whitelist");
    }

    public String getConsoleName() {
        return plugin.getConfig().getString("display.console-name", "Console");
    }

    public boolean isUseDisplayNames() {
        return plugin.getConfig().getBoolean("display.use-display-names", false);
    }

    public boolean isDefaultSilent() {
        return plugin.getConfig().getBoolean("display.default-silent", false);
    }

    public boolean isDefaultIp() {
        return plugin.getConfig().getBoolean("display.default-ip", false);
    }
}
