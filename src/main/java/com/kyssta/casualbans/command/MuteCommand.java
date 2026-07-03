package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.CommandFlags;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MuteCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.mute")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/mute <player> [-s] [-S] [-p] [-I] [server:scope] [duration] [reason]");
            return true;
        }

        CommandFlags.ParsedResult parsed = parseFlags(args);
        CommandFlags flags = parsed.flags();
        String[] remaining = parsed.args();

        if (remaining.length < 1) {
            sendUsage(sender, "/mute <player> [-s] [-S] [-p] [-I] [server:scope] [duration] [reason]");
            return true;
        }

        String targetName = remaining[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        // Check exempt
        if (isExempt(targetUUID, "mute", sender)) {
            MessageUtil.sendError(sender, "That player is exempt from being muted.");
            return true;
        }

        // Determine duration and reason
        long duration = -1; // permanent
        String reason = null;
        int reasonStart = 1;

        if (remaining.length > 1) {
            String possibleDuration = remaining[1];
            long parsedDuration = TimeUtil.parseDuration(possibleDuration);
            if (parsedDuration > 0) {
                duration = parsedDuration;
                reasonStart = 2;
            }
        }

        if (remaining.length > reasonStart) {
            StringBuilder sb = new StringBuilder();
            for (int i = reasonStart; i < remaining.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(remaining[i]);
            }
            reason = sb.toString();
        }

        // Check duration limits
        if (duration > 0) {
            long maxDuration = plugin.getStaffManager().getMaxDuration(sender, PunishmentType.MUTE);
            if (maxDuration > 0 && duration > maxDuration) {
                MessageUtil.sendError(sender, "Duration exceeds your maximum allowed mute duration (" +
                    TimeUtil.formatDuration(maxDuration) + ").");
                return true;
            }
        }

        // Check cooldown
        if (!plugin.getStaffManager().hasCooldownBypass(sender)) {
            int cooldown = plugin.getStaffManager().getCooldownSeconds(sender, PunishmentType.MUTE);
            if (cooldown > 0) {
                // Cooldown check handled by manager
            }
        }

        // Check mute permissions for flags
        if (flags.isSilent() && !sender.hasPermission("casualbans.silent")) {
            MessageUtil.sendError(sender, "You do not have permission to use silent mode.");
            return true;
        }

        plugin.getPunishmentManager().punish(sender, targetUUID, targetName,
            PunishmentType.MUTE, duration, reason, flags);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
