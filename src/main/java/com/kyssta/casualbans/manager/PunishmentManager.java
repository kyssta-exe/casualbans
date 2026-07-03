package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.CommandFlags;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.model.Template;
import com.kyssta.casualbans.storage.StorageProvider;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Central manager for creating, removing, and querying punishments
 * across all types (bans, mutes, warns, kicks).
 */
public class PunishmentManager {

    private final CasualBans plugin;

    public PunishmentManager(CasualBans plugin) {
        this.plugin = plugin;
    }

    // ═══════════════════════════════════════════════════════════════
    //   Create Punishments
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ban a player permanently.
     */
    public Punishment ban(UUID uuid, String name, String reason,
                          UUID executorUUID, String executorName,
                          CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.BAN, uuid, name, reason, -1,
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * Temp-ban a player for a specified duration.
     */
    public Punishment tempban(UUID uuid, String name, String reason, long duration,
                              UUID executorUUID, String executorName,
                              CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.TEMPBAN, uuid, name, reason, duration,
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * IP-ban a player permanently (bans both the player and their IP).
     */
    public Punishment ipban(UUID uuid, String name, String reason,
                            UUID executorUUID, String executorName,
                            CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.IPBAN, uuid, name, reason, -1,
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * Mute a player permanently.
     */
    public Punishment mute(UUID uuid, String name, String reason,
                           UUID executorUUID, String executorName,
                           CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.MUTE, uuid, name, reason, -1,
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * Temp-mute a player for a specified duration.
     */
    public Punishment tempmute(UUID uuid, String name, String reason, long duration,
                               UUID executorUUID, String executorName,
                               CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.TEMPMUTE, uuid, name, reason, duration,
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * IP-mute a player (mutes both the player and their IP).
     */
    public Punishment ipmute(UUID uuid, String name, String reason,
                             UUID executorUUID, String executorName,
                             CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.IPMUTE, uuid, name, reason, -1,
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * Warn a player (creates a warning entry).
     */
    public Punishment warn(UUID uuid, String name, String reason,
                           UUID executorUUID, String executorName,
                           CommandFlags flags, String serverScope) {
        return applyPunishment(PunishmentType.WARN, uuid, name, reason,
            plugin.getConfigManager().getWarnExpireAfter(),
            executorUUID, executorName, flags, serverScope, null);
    }

    /**
     * Kick a player (kicks without creating a lasting punishment record).
     */
    public Punishment kick(UUID uuid, String name, String reason,
                           UUID executorUUID, String executorName,
                           CommandFlags flags, String serverScope) {
        Punishment punishment = buildPunishment(PunishmentType.KICK, uuid, name, reason, 0,
            executorUUID, executorName, flags, serverScope, null);

        // Kick the player if online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            String kickMsg = formatKickMessage(punishment);
            player.kickPlayer(MessageUtil.legacy(kickMsg));
        }

        // Broadcast the kick
        broadcastPunishment(punishment, "kick.broadcast", "kick.broadcast-no-reason");

        return punishment;
    }

    // ═══════════════════════════════════════════════════════════════
    //   Template-based Punishments
    // ═══════════════════════════════════════════════════════════════

    /**
     * Apply a punishment using a template. Looks up the next ladder step
     * based on the player's offense count and applies it with the template's
     * configured duration, reason, flags, and messages.
     *
     * @param uuid          the target UUID
     * @param name          the target's current name
     * @param templateName  the template name to use
     * @param executorUUID  the executor UUID
     * @param executorName  the executor name
     * @param flags         command flags (may override template defaults)
     * @param serverScope   the server scope
     * @return the created punishment, or null if the template wasn't found
     */
    public Punishment applyTemplate(UUID uuid, String name, String templateName,
                                    UUID executorUUID, String executorName,
                                    CommandFlags flags, String serverScope) {
        TemplateManager tm = plugin.getTemplateManager();
        Template template = tm.getTemplate(templateName, PunishmentType.BAN);
        if (template == null) template = tm.getTemplate(templateName, PunishmentType.MUTE);
        if (template == null) template = tm.getTemplate(templateName, PunishmentType.WARN);
        if (template == null) template = tm.getTemplate(templateName, PunishmentType.KICK);
        if (template == null) return null;

        // Determine offense count (IP-shared or per-player)
        int offenseCount;
        String ip = null;
        if (template.isIpTemplate()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getAddress() != null) {
                ip = player.getAddress().getAddress().getHostAddress();
            }
            offenseCount = ip != null ? tm.getOffenseCountByIP(ip, templateName) : 0;
        } else {
            offenseCount = tm.getOffenseCount(uuid, templateName);
        }

        // Resolve the applicable ladder step
        var step = template.getStep(offenseCount);

        // Use step overrides, falling back to template defaults
        String reason = step != null && step.getReason() != null ? step.getReason() : template.getReason();
        long duration = resolveDuration(template, step);
        PunishmentType type = template.getType();

        return applyPunishment(type, uuid, name,
            reason != null ? reason : flags != null ? flags.getTemplateName() : "No reason provided",
            duration, executorUUID, executorName, flags, serverScope, templateName);
    }

    // ═══════════════════════════════════════════════════════════════
    //   Remove Punishments
    // ═══════════════════════════════════════════════════════════════

    /**
     * Unban a player (deactivates the active ban).
     *
     * @return the deactivated ban punishment, or null if none was active
     */
    public Punishment unban(UUID uuid, String serverScope,
                            UUID removedByUUID, String removedByName, String removalReason) {
        Punishment ban = getActiveBan(uuid, serverScope);
        if (ban == null) return null;

        ban.setActive(false);
        ban.setRemovedByUUID(removedByUUID);
        ban.setRemovedByName(removedByName);
        ban.setRemovalReason(removalReason);
        plugin.getStorageProvider().updatePunishment(ban);

        broadcastRemoval(ban, PunishmentType.UNBAN, "ban.unban-broadcast");
        plugin.getSyncManager().notifyPunishmentRemoved(ban);

        return ban;
    }

    /**
     * Unmute a player (deactivates the active mute).
     *
     * @return the deactivated mute punishment, or null if none was active
     */
    public Punishment unmute(UUID uuid, String serverScope,
                             UUID removedByUUID, String removedByName, String removalReason) {
        Punishment mute = getActiveMute(uuid, serverScope);
        if (mute == null) return null;

        mute.setActive(false);
        mute.setRemovedByUUID(removedByUUID);
        mute.setRemovedByName(removedByName);
        mute.setRemovalReason(removalReason);
        plugin.getStorageProvider().updatePunishment(mute);

        broadcastRemoval(mute, PunishmentType.UNMUTE, "mute.unmute-broadcast");
        plugin.getSyncManager().notifyPunishmentRemoved(mute);

        return mute;
    }

    /**
     * Unwarn a player (deactivates the most recent active warning).
     *
     * @return the deactivated warning, or null if no active warning
     */
    public Punishment unwarn(UUID uuid, UUID removedByUUID, String removedByName) {
        List<Punishment> warnings = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.WARN, plugin.getDefaultServerScope());

        if (warnings.isEmpty()) return null;

        // Remove the most recent active warning
        Punishment warn = warnings.get(warnings.size() - 1);
        warn.setActive(false);
        warn.setRemovedByUUID(removedByUUID);
        warn.setRemovedByName(removedByName);
        warn.setRemovalReason("Unwarned");
        plugin.getStorageProvider().updatePunishment(warn);

        broadcastRemoval(warn, PunishmentType.UNWARN, "warn.unwarn-broadcast");
        plugin.getSyncManager().notifyPunishmentRemoved(warn);

        return warn;
    }

    // ═══════════════════════════════════════════════════════════════
    //   Query Methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if a player is currently banned on the given scope.
     */
    public boolean isBanned(UUID uuid, String serverScope) {
        return plugin.getStorageProvider().isPlayerBanned(uuid, serverScope);
    }

    /**
     * Check if a player is currently muted on the given scope.
     */
    public boolean isMuted(UUID uuid, String serverScope) {
        return plugin.getStorageProvider().isPlayerMuted(uuid, serverScope);
    }

    /**
     * Check if a player has any active warnings.
     */
    public boolean isWarned(UUID uuid) {
        return plugin.getStorageProvider().isPlayerWarned(uuid, plugin.getDefaultServerScope());
    }

    /**
     * Get the active ban for a player on the given scope.
     *
     * @return the active ban punishment, or null if none
     */
    public Punishment getActiveBan(UUID uuid, String serverScope) {
        List<Punishment> bans = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.BAN, serverScope);
        if (!bans.isEmpty()) return bans.get(0);

        // Also check TEMPBAN and IPBAN
        bans = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.TEMPBAN, serverScope);
        if (!bans.isEmpty()) return bans.get(0);

        bans = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.IPBAN, serverScope);
        return bans.isEmpty() ? null : bans.get(0);
    }

    /**
     * Get the active mute for a player on the given scope.
     *
     * @return the active mute punishment, or null if none
     */
    public Punishment getActiveMute(UUID uuid, String serverScope) {
        List<Punishment> mutes = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.MUTE, serverScope);
        if (!mutes.isEmpty()) return mutes.get(0);

        mutes = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.TEMPMUTE, serverScope);
        if (!mutes.isEmpty()) return mutes.get(0);

        mutes = plugin.getStorageProvider()
            .getActivePunishments(uuid, PunishmentType.IPMUTE, serverScope);
        return mutes.isEmpty() ? null : mutes.get(0);
    }

    /**
     * Get all active warnings for a player.
     */
    public List<Punishment> getActiveWarnings(UUID uuid) {
        return plugin.getStorageProvider().getActivePunishments(uuid, PunishmentType.WARN,
            plugin.getDefaultServerScope());
    }

    /**
     * Alias used by command handlers for warnings.
     */
    public List<Punishment> getWarnings(UUID uuid, String serverScope) {
        return plugin.getStorageProvider().getActivePunishments(uuid, PunishmentType.WARN, serverScope);
    }

    /**
     * Get warning count for a player.
     */
    public int getWarningCount(UUID uuid, String serverScope) {
        List<Punishment> warnings = getWarnings(uuid, serverScope);
        return warnings != null ? warnings.size() : 0;
    }

    /**
     * Get staff history filtered by type.
     */
    public List<Punishment> getStaffHistoryByType(UUID executorUUID, PunishmentType type, int limit) {
        return plugin.getStorageProvider().getStaffHistoryByType(executorUUID, type, limit);
    }

    /**
     * Check if an IP address is banned on the given scope.
     */
    public boolean isIPBanned(String ip, String serverScope) {
        return plugin.getStorageProvider().isIPBanned(ip, serverScope);
    }

    /**
     * Get all active punishments of a given type on a scope.
     */
    public List<Punishment> getAllActive(PunishmentType type, String serverScope) {
        return plugin.getStorageProvider().getAllActivePunishments(type, serverScope);
    }

    /**
     * Alias used by command handlers.
     */
    public List<Punishment> getActivePunishments(PunishmentType type, String serverScope) {
        return getAllActive(type, serverScope);
    }

    /**
     * Get punishment history for a player.
     */
    public List<Punishment> getHistory(UUID uuid, int limit) {
        return plugin.getStorageProvider().getHistory(uuid, limit);
    }

    /**
     * Get punishment history for a player filtered by type.
     */
    public List<Punishment> getHistoryByType(UUID uuid, PunishmentType type, int limit) {
        return plugin.getStorageProvider().getHistoryByType(uuid, type, limit);
    }

    /**
     * Get staff action history.
     */
    public List<Punishment> getStaffHistory(UUID executorUUID, int limit) {
        return plugin.getStorageProvider().getStaffHistory(executorUUID, limit);
    }

    /**
     * Reload active state from storage (e.g., for expired punishment cleanup).
     */
    public void reload() {
        // Future: re-check all active punishments for expiry
    }

    // ═══════════════════════════════════════════════════════════════
    //   Internal
    // ═══════════════════════════════════════════════════════════════

    /**
     * Central method for applying any punishment type. Builds the Punishment
     * object, persists it, broadcasts it (unless extra-silent), kicks the
     * player if it's a ban-type, and notifies cross-server sync.
     */
    private Punishment applyPunishment(PunishmentType type, UUID uuid, String name,
                                       String reason, long duration,
                                       UUID executorUUID, String executorName,
                                       CommandFlags flags, String serverScope,
                                       String templateName) {
        Punishment punishment = buildPunishment(type, uuid, name, reason, duration,
            executorUUID, executorName, flags, serverScope, templateName);

        // Save to storage
        plugin.getStorageProvider().savePunishment(punishment);

        // Broadcast (skip for extra-silent)
        if (flags == null || !flags.isExtraSilent()) {
            broadcastPunishment(punishment, flags);
        }

        // Sync to other servers
        plugin.getSyncManager().notifyPunishmentCreated(punishment);

        // Discord webhook
        CasualBans.getInstance().getWebhookManager().sendPunishmentWebhook(punishment);

        // Kick player if it's a ban-type or kick-type punishment
        if (type.isBan() || type == PunishmentType.KICK) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                String kickMsg = formatKickMessage(punishment);
                player.kickPlayer(MessageUtil.legacy(kickMsg));
            }
        }

        return punishment;
    }

    /**
     * Build a Punishment object from method parameters.
     */
    private Punishment buildPunishment(PunishmentType type, UUID uuid, String name,
                                       String reason, long duration,
                                       UUID executorUUID, String executorName,
                                       CommandFlags flags, String serverScope,
                                       String templateName) {
        // Resolve server scope
        String scope = serverScope;
        if (scope == null || scope.isEmpty()) {
            scope = plugin.getDefaultServerScope();
        }
        if (flags != null && flags.isGlobal()) {
            scope = "*";
        }
        if (flags != null && flags.getServerScope() != null) {
            scope = flags.getServerScope();
        }

        // Resolve silence
        boolean silent = flags != null && flags.isSilent();
        boolean ipPunish = flags != null && flags.isIpPunish() || type.isIP();

        // Determine start/end times
        long now = System.currentTimeMillis();
        long end = duration > 0 ? now + duration : 0;

        // Get IP if it's an IP punish
        String ip = null;
        if (ipPunish || type.isIP()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getAddress() != null) {
                ip = player.getAddress().getAddress().getHostAddress();
            }
        }

        return Punishment.builder()
            .type(type)
            .uuid(uuid)
            .ip(ip)
            .name(name)
            .reason(reason)
            .executorUUID(executorUUID)
            .executorName(executorName)
            .dateStart(now)
            .dateEnd(end)
            .duration(duration)
            .serverScope(scope)
            .serverOrigin(plugin.getServerName())
            .silent(silent)
            .ipPunish(ipPunish)
            .active(true)
            .templateName(templateName)
            .build();
    }

    /**
     * Broadcast a punishment creation notification.
     */
    private void broadcastPunishment(Punishment punishment, CommandFlags flags) {
        String broadcastKey;
        String fallbackKey = null;

        switch (punishment.getType()) {
            case BAN:
                broadcastKey = punishment.isIpPunish() ? "ban.ip-broadcast" : "ban.broadcast";
                break;
            case TEMPBAN:
                broadcastKey = punishment.isIpPunish() ? "ban.temp-ip-broadcast" : "ban.temp-broadcast";
                break;
            case IPBAN:
                broadcastKey = "ban.ip-broadcast";
                break;
            case MUTE:
                broadcastKey = punishment.isIpPunish() ? "mute.ip-broadcast" : "mute.broadcast";
                break;
            case TEMPMUTE:
                broadcastKey = punishment.isIpPunish() ? "mute.ip-broadcast" : "mute.broadcast";
                break;
            case IPMUTE:
                broadcastKey = "mute.ip-broadcast";
                break;
            case WARN:
                broadcastKey = "warn.broadcast";
                break;
            case KICK:
                broadcastKey = "kick.broadcast";
                fallbackKey = "kick.broadcast-no-reason";
                break;
            default:
                return;
        }

        broadcastPunishment(punishment, broadcastKey, fallbackKey);
    }

    /**
     * Broadcast a punishment message, handling silent / extra-silent flags.
     */
    private void broadcastPunishment(Punishment punishment, String messageKey, String fallbackKey) {
        boolean silent = punishment.isSilent();
        String msg = MessageUtil.getMessage(messageKey,
            "$player", punishment.getName() != null ? punishment.getName() : "Unknown",
            "$executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Console",
            "$reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided",
            "$duration", punishment.getDurationString(),
            "$originalDuration", punishment.getOriginalDurationString(),
            "$serverScope", punishment.getServerScope() != null ? punishment.getServerScope() : "*",
            "$serverOrigin", punishment.getServerOrigin() != null ? punishment.getServerOrigin() : "Unknown",
            "$id", String.valueOf(punishment.getId()),
            "$silent", silent ? MessageUtil.getMessage("silent-prefix", "$silent", "") : "",
            "$ipban", punishment.isIpPunish() ? "IP" : "");

        // If message is empty (configured as ''), try fallback
        if (msg.isEmpty() || msg.equals(messageKey)) {
            if (fallbackKey != null) {
                msg = MessageUtil.getMessage(fallbackKey,
                    "$player", punishment.getName(),
                    "$executor", punishment.getExecutorName(),
                    "$reason", punishment.getReason());
            }
        }

        if (msg.isEmpty() || msg.equals(messageKey) || msg.equals(fallbackKey)) {
            return; // No configured message
        }

        // Apply silent prefix if needed
        if (silent) {
            msg = MessageUtil.getMessage("silent-prefix", "$silent", "") + msg;
        }

        // Broadcast
        if (silent) {
            MessageUtil.broadcast(msg, "casualbans.silent");
        } else {
            MessageUtil.broadcast(msg, null);
        }
    }

    /**
     * Broadcast a punishment removal notification.
     */
    private void broadcastRemoval(Punishment punishment, PunishmentType removalType, String messageKey) {
        String removedByName = punishment.getRemovedByName() != null ? punishment.getRemovedByName() : "Console";
        String removalReason = punishment.getRemovalReason() != null ? punishment.getRemovalReason() : "No reason specified";

        String msg = MessageUtil.getMessage(messageKey,
            "$player", punishment.getName() != null ? punishment.getName() : "Unknown",
            "$executor", removedByName,
            "$removalReason", removalReason);

        if (msg.isEmpty() || msg.equals(messageKey)) return;

        MessageUtil.broadcast(msg, null);
        plugin.getSyncManager().notifyPunishmentRemoved(punishment);
    }

    /**
     * Format the kick screen message for a ban or kick punishment.
     */
    private String formatKickMessage(Punishment punishment) {
        String key;

        switch (punishment.getType()) {
            case BAN, TEMPBAN, IPBAN:
                key = punishment.isPermanent() ? "ban.kick-message-permanent" : "ban.kick-message";
                break;
            case KICK:
                key = punishment.getReason() != null ? "kick.message" : "kick.message-no-reason";
                break;
            default:
                return "You have been removed from the server.";
        }

        return MessageUtil.getMessage(key,
            "$player", punishment.getName() != null ? punishment.getName() : "Unknown",
            "$executor", punishment.getExecutorName() != null ? punishment.getExecutorName() : "Console",
            "$reason", punishment.getReason() != null ? punishment.getReason() : "No reason provided",
            "$duration", punishment.getDurationString());
    }

    /**
     * Resolve effective duration from template + ladder step.
     */
    private long resolveDuration(com.kyssta.casualbans.model.Template template,
                                  com.kyssta.casualbans.model.TemplateLadderStep step) {
        if (step != null && step.getDuration() > 0) {
            return step.getDuration();
        }
        if (step != null && step.getDuration() == -1) {
            return -1; // permanent override
        }
        return template.getDuration();
    }

    // ═══════════════════════════════════════════
    //  Convenience methods for command handlers
    // ═══════════════════════════════════════════

    /**
     * Unified punish method that resolves sender info and delegates to the correct handler.
     */
    public Punishment punish(CommandSender sender, UUID targetUUID, String targetName,
                             PunishmentType type, long duration, String reason, CommandFlags flags) {
        UUID executorUUID = sender instanceof Player p ? p.getUniqueId() : UUID.nameUUIDFromBytes("CONSOLE".getBytes());
        String executorName = sender instanceof Player p ? p.getName() : "Console";
        String scope = flags != null && flags.getServerScope() != null ? flags.getServerScope() : plugin.getDefaultServerScope();

        // Override type for IP punishments if flag is set
        if (flags != null && flags.isIpPunish() && !type.isIP()) {
            type = switch (type) {
                case BAN, TEMPBAN -> PunishmentType.IPBAN;
                case MUTE, TEMPMUTE -> PunishmentType.IPMUTE;
                default -> type;
            };
        }

        return applyPunishment(type, targetUUID, targetName, reason, duration,
            executorUUID, executorName, flags, scope, null);
    }

    /**
     * Get cached mute for fast synchronous check (used in chat listener).
     */
    public Punishment getCachedMute(UUID uuid) {
        return getActiveMute(uuid, "*");
    }

    /**
     * Remove (deactivate) a punishment - used by /unban, /unmute, /unwarn commands.
     */
    public Punishment removePunishment(CommandSender sender, UUID targetUUID,
                                       PunishmentType removalType, String reason, CommandFlags flags) {
        UUID removerUUID = sender instanceof Player p ? p.getUniqueId() : UUID.nameUUIDFromBytes("CONSOLE".getBytes());
        String removerName = sender instanceof Player p ? p.getName() : "Console";

        return switch (removalType) {
            case UNBAN -> unban(targetUUID, plugin.getDefaultServerScope(), removerUUID, removerName, reason);
            case UNMUTE -> unmute(targetUUID, plugin.getDefaultServerScope(), removerUUID, removerName, reason);
            case UNWARN -> unwarn(targetUUID, removerUUID, removerName);
            default -> null;
        };
    }
}
