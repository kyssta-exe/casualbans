package com.kyssta.casualbans.command;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.CommandFlags;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Base class for all CasualBans commands providing common utility methods.
 */
public abstract class BaseCommand {

    protected final CasualBans plugin;

    protected BaseCommand() {
        this.plugin = CasualBans.getInstance();
    }

    /**
     * Resolve an online player by name.
     *
     * @param name the player name
     * @return the Player, or null if not found
     */
    protected Player getPlayer(String name) {
        if (name == null || name.isEmpty()) return null;
        return Bukkit.getPlayerExact(name);
    }

    /**
     * Resolve a UUID from a player name or UUID string.
     *
     * @param input the player name or UUID string
     * @return the UUID, or null if unresolvable
     */
    protected UUID resolveUUID(String input) {
        return UUIDUtil.resolveUUID(input);
    }

    /**
     * Check if a sender has a specific permission.
     *
     * @param sender     the command sender
     * @param permission the permission node
     * @return true if the sender has the permission
     */
    protected boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        MessageUtil.sendError(sender, "You do not have permission to use this command.");
        return false;
    }

    /**
     * Send usage information to the sender.
     *
     * @param sender the command sender
     * @param usage  the usage string
     */
    protected void sendUsage(CommandSender sender, String usage) {
        MessageUtil.sendPrefix(sender, "<yellow>Usage: </yellow><white>" + usage + "</white>");
    }

    /**
     * Parse flags from command arguments.
     *
     * @param args the raw command arguments
     * @return the parsed result containing flags and remaining args
     */
    protected CommandFlags.ParsedResult parseFlags(String[] args) {
        return CommandFlags.parse(args);
    }

    /**
     * Get the server scope from flags, falling back to default.
     *
     * @param flags the parsed command flags
     * @return the server scope string
     */
    protected String getServerScope(CommandFlags flags) {
        if (flags.getServerScope() != null) return flags.getServerScope();
        return plugin.getDefaultServerScope();
    }

    /**
     * Broadcast a punishment message considering silent/extra-silent flags.
     *
     * @param message the message to broadcast
     * @param flags   the command flags
     */
    protected void broadcastPunishment(String message, CommandFlags flags) {
        if (flags.isExtraSilent()) {
            // Extra silent: only console and casualbans.notify.silent
            MessageUtil.broadcast(message, "casualbans.notify.silent");
        } else if (flags.isSilent()) {
            // Silent: staff with notify permission
            MessageUtil.broadcast(message, "casualbans.notify");
        } else {
            // Normal: everyone with broadcast permission + console
            MessageUtil.broadcast(message, "casualbans.notify.broadcast");
        }
    }

    /**
     * Check if a target player is exempt from the given punishment type.
     *
     * @param targetUuid the target player's UUID
     * @param type       the punishment type string (ban, mute, warn, kick)
     * @param sender     the executor
     * @return true if the target is exempt
     */
    protected boolean isExempt(UUID targetUuid, String type, CommandSender sender) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) return false;

        // Check global exempt
        if (target.hasPermission("casualbans.exempt")) return true;
        // Check type-specific exempt
        if (target.hasPermission("casualbans.exempt." + type)) return true;

        return false;
    }
}
