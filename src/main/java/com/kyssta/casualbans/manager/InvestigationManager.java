package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.storage.StorageProvider;
import com.kyssta.casualbans.util.UUIDUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles alt account detection, IP tracking, and name history.
 */
public class InvestigationManager {

    private final CasualBans plugin;

    public InvestigationManager(CasualBans plugin) {
        this.plugin = plugin;
    }

    /**
     * Get all accounts associated with a player's IP address(es).
     * Returns a map of IP to list of account info.
     */
    public Map<String, List<AccountInfo>> getDupeIPAccounts(UUID uuid) {
        Map<String, List<AccountInfo>> results = new LinkedHashMap<>();
        List<String> ips = plugin.getStorageProvider().getIPsByUUID(uuid);

        int limit = plugin.getConfig().getInt("notifications.dupeip-scan-limit", 20);
        boolean showMuted = plugin.getConfig().getBoolean("notifications.dupeip-show-muted-accounts", false);

        for (String ip : ips) {
            List<UUID> accounts = plugin.getStorageProvider().getAccountsByIP(ip);
            List<AccountInfo> infoList = accounts.stream()
                .filter(altUuid -> !altUuid.equals(uuid))
                .limit(limit)
                .map(altUuid -> {
                    String name = UUIDUtil.getName(altUuid);
                    boolean banned = plugin.getPunishmentManager().isBanned(altUuid, "*");
                    boolean muted = showMuted && plugin.getPunishmentManager().isMuted(altUuid, "*");
                    boolean online = org.bukkit.Bukkit.getPlayer(altUuid) != null;
                    return new AccountInfo(altUuid, name, banned, muted, online);
                })
                .collect(Collectors.toList());

            if (!infoList.isEmpty()) {
                results.put(ip, infoList);
            }
        }

        return results;
    }

    /**
     * Get IP history records for a player.
     */
    public List<StorageProvider.IPRecord> getIPHistory(UUID uuid) {
        return plugin.getStorageProvider().getIPHistory(uuid);
    }

    /**
     * Get all IP addresses a player has used.
     */
    public List<String> getIPsByUUID(UUID uuid) {
        return plugin.getStorageProvider().getIPsByUUID(uuid);
    }

    /**
     * Get name history records for a player.
     */
    public List<StorageProvider.NameRecord> getNameHistory(UUID uuid) {
        return plugin.getStorageProvider().getNameHistory(uuid);
    }

    /**
     * Get all accounts by IP address (delegates to storage).
     */
    public List<UUID> getAccountsByIP(String ip) {
        return plugin.getStorageProvider().getAccountsByIP(ip);
    }

    /**
     * Scan all online players for duplicate IPs.
     */
    public Map<UUID, List<UUID>> getIPReport() {
        Map<UUID, List<UUID>> report = new LinkedHashMap<>();
        Map<String, List<UUID>> ipToPlayers = new HashMap<>();

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getAddress() == null) continue;
            String ip = player.getAddress().getAddress().getHostAddress();
            ipToPlayers.computeIfAbsent(ip, k -> new ArrayList<>()).add(player.getUniqueId());
        }

        for (Map.Entry<String, List<UUID>> entry : ipToPlayers.entrySet()) {
            if (entry.getValue().size() >= plugin.getConfig().getInt("ipreport-minimum-accounts", 2)) {
                for (UUID uuid : entry.getValue()) {
                    report.put(uuid, entry.getValue().stream()
                        .filter(u -> !u.equals(uuid))
                        .collect(Collectors.toList()));
                }
            }
        }

        return report;
    }

    /**
     * Get the last known IP address for a player.
     */
    public String getLastIP(UUID uuid) {
        var history = plugin.getStorageProvider().getIPHistory(uuid);
        if (history == null || history.isEmpty()) return "Unknown";
        return history.get(history.size() - 1).ip();
    }

    public record AccountInfo(UUID uuid, String name, boolean banned, boolean muted, boolean online) {}
}
