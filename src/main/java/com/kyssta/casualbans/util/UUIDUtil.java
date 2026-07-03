package com.kyssta.casualbans.util;

import com.kyssta.casualbans.CasualBans;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public final class UUIDUtil {

    private static final Map<String, UUID> NAME_CACHE = new HashMap<>();
    private static final Map<UUID, String> UUID_NAME_CACHE = new HashMap<>();

    private UUIDUtil() {}

    /**
     * Resolve a UUID from a player name or UUID string.
     */
    public static UUID resolveUUID(String input) {
        if (input == null || input.isEmpty()) return null;

        // Try UUID format directly
        if (input.contains("-")) {
            try {
                return UUID.fromString(input);
            } catch (IllegalArgumentException ignored) {}
        }

        // Try 32-char hex
        if (input.length() == 32) {
            try {
                String withDashes = input.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
                );
                return UUID.fromString(withDashes);
            } catch (IllegalArgumentException ignored) {}
        }

        // Try online player
        org.bukkit.entity.Player player = Bukkit.getPlayerExact(input);
        if (player != null) {
            cacheName(player.getUniqueId(), player.getName());
            return player.getUniqueId();
        }

        // Try offline player
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(input);
        if (offline != null && offline.hasPlayedBefore()) {
            cacheName(offline.getUniqueId(), offline.getName());
            return offline.getUniqueId();
        }

        // Try lookup (blocking - may return a UUID even if never played)
        try {
            OfflinePlayer offlinePl = Bukkit.getOfflinePlayer(input);
            if (offlinePl.hasPlayedBefore()) {
                cacheName(offlinePl.getUniqueId(), offlinePl.getName());
                return offlinePl.getUniqueId();
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Get the current name for a UUID.
     */
    public static String getName(UUID uuid) {
        if (uuid == null) return "Unknown";
        String cached = UUID_NAME_CACHE.get(uuid);
        if (cached != null) return cached;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            cacheName(uuid, player.getName());
            return player.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null) {
            cacheName(uuid, offline.getName());
            return offline.getName();
        }
        return uuid.toString().substring(0, 8);
    }

    public static void cacheName(UUID uuid, String name) {
        if (uuid != null && name != null) {
            NAME_CACHE.put(name.toLowerCase(), uuid);
            UUID_NAME_CACHE.put(uuid, name);
        }
    }

    public static void clearCache() {
        NAME_CACHE.clear();
        UUID_NAME_CACHE.clear();
    }
}
