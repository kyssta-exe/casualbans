package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.model.StaffGroup;
import com.kyssta.casualbans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages staff permission groups, duration limits, cooldowns, and hierarchical exemption.
 */
public class StaffManager {

    private final CasualBans plugin;
    private final Map<String, StaffGroup> groups = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public StaffManager(CasualBans plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        groups.clear();
        cooldowns.clear();
        loadGroups();
    }

    private void loadGroups() {
        var limitsSection = plugin.getConfig().getConfigurationSection("durations.limits");
        if (limitsSection == null) return;

        for (String key : limitsSection.getKeys(false)) {
            var s = limitsSection.getConfigurationSection(key);
            if (s == null) continue;

            StaffGroup group = StaffGroup.builder()
                .name(key)
                .permission(s.getString("permission", ""))
                .maxTempBanDuration(TimeUtil.parseDuration(s.getString("tempban", "permanent")))
                .maxTempMuteDuration(TimeUtil.parseDuration(s.getString("tempmute", "permanent")))
                .cooldownBan(parseCooldown(s.getString("cooldown-ban", "0s")))
                .cooldownMute(parseCooldown(s.getString("cooldown-mute", "0s")))
                .cooldownWarn(parseCooldown(s.getString("cooldown-warn", "0s")))
                .cooldownKick(parseCooldown(s.getString("cooldown-kick", "0s")))
                .cooldownRedo(parseCooldown(s.getString("cooldown-redo", "0s")))
                .requireTemplate(s.getBoolean("require-template", false))
                .weight(s.getInt("weight", 0))
                .build();

            groups.put(key, group);
        }
    }

    private int parseCooldown(String input) {
        if (input == null || input.equals("0s") || input.equals("0")) return 0;
        return (int) (TimeUtil.parseDuration(input) / 1000);
    }

    /**
     * Get the applicable staff group for a player.
     */
    public StaffGroup getGroup(Player player) {
        if (player.hasPermission("casualbans.group.unlimited")) {
            return StaffGroup.builder()
                .name("unlimited")
                .maxTempBanDuration(-1)
                .maxTempMuteDuration(-1)
                .requireTemplate(false)
                .weight(Integer.MAX_VALUE)
                .build();
        }

        return groups.values().stream()
            .filter(g -> g.getPermission() != null && !g.getPermission().isEmpty())
            .filter(g -> player.hasPermission(g.getPermission()))
            .findFirst()
            .orElseGet(() -> groups.get("default"));
    }

    /**
     * Get the maximum tempban duration for a player.
     */
    public long getMaxTempBanDuration(Player player) {
        if (player.hasPermission("casualbans.group.unlimited")) return -1;
        return groups.values().stream()
            .filter(g -> g.getPermission() == null || g.getPermission().isEmpty() || player.hasPermission(g.getPermission()))
            .map(StaffGroup::getMaxTempBanDuration)
            .filter(d -> d > 0)
            .min(Long::compareTo)
            .orElse(-1L);
    }

    public long getMaxTempMuteDuration(Player player) {
        if (player.hasPermission("casualbans.group.unlimited")) return -1;
        return groups.values().stream()
            .filter(g -> g.getPermission() == null || g.getPermission().isEmpty() || player.hasPermission(g.getPermission()))
            .map(StaffGroup::getMaxTempMuteDuration)
            .filter(d -> d > 0)
            .min(Long::compareTo)
            .orElse(-1L);
    }

    // ── Methods used by command handlers ──

    /**
     * Get max duration for a punishment type from CommandSender.
     */
    public long getMaxDuration(CommandSender sender, PunishmentType type) {
        if (!(sender instanceof Player player)) return -1;
        if (player.hasPermission("casualbans.group.unlimited")) return -1;
        if (type == PunishmentType.TEMPBAN || type == PunishmentType.BAN) {
            return getMaxTempBanDuration(player);
        }
        if (type == PunishmentType.TEMPMUTE || type == PunishmentType.MUTE) {
            return getMaxTempMuteDuration(player);
        }
        return -1;
    }

    /**
     * Check if sender bypasses cooldowns.
     */
    public boolean hasCooldownBypass(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission("casualbans.cooldown.bypass");
    }

    /**
     * Get cooldown seconds for a sender and punishment type.
     */
    public int getCooldownSeconds(CommandSender sender, PunishmentType type) {
        if (!(sender instanceof Player player)) return 0;
        return getRemainingCooldown(player, type) > 0
            ? (int)(getRemainingCooldown(player, type) / 1000)
            : 0;
    }

    /**
     * Check if a player is on cooldown for a punishment type.
     */
    public boolean isOnCooldown(Player player, PunishmentType type) {
        if (player.hasPermission("casualbans.cooldown.bypass")) return false;

        var playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        String key = type.getBaseType().name().toLowerCase();
        Long expiry = playerCooldowns.get(key);
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /**
     * Apply cooldown for a punishment type.
     */
    public void applyCooldown(Player player, PunishmentType type) {
        StaffGroup group = getGroup(player);
        int cooldownSeconds = switch (type.getBaseType()) {
            case BAN -> group.getCooldownBan();
            case MUTE -> group.getCooldownMute();
            case WARN -> group.getCooldownWarn();
            case KICK -> group.getCooldownKick();
            default -> 0;
        };

        if (cooldownSeconds <= 0) return;

        long expiry = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
            .put(type.getBaseType().name().toLowerCase(), expiry);
    }

    /**
     * Get remaining cooldown time for a player.
     */
    public long getRemainingCooldown(Player player, PunishmentType type) {
        var playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long expiry = playerCooldowns.get(type.getBaseType().name().toLowerCase());
        if (expiry == null) return 0;

        return Math.max(0, expiry - System.currentTimeMillis());
    }

    /**
     * Check hierarchical exemption - can sender punish target?
     */
    public boolean canPunish(Player sender, Player target) {
        if (!plugin.getConfig().getBoolean("exempt.use-group-weights", false)) return true;
        if (sender.hasPermission("casualbans.exempt.bypass")) return true;

        int senderWeight = getGroup(sender).getWeight();
        int targetWeight = getGroup(target).getWeight();

        return senderWeight > targetWeight;
    }
}
