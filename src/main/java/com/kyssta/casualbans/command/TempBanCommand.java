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

public class TempBanCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.tempban")) return true;

        if (args.length < 2) {
            sendUsage(sender, "/tempban <player> [-s] [-S] [-p] [-I] [server:scope] <duration> [reason]");
            return true;
        }

        CommandFlags.ParsedResult parsed = parseFlags(args);
        CommandFlags flags = parsed.flags();
        String[] remaining = parsed.args();

        if (remaining.length < 2) {
            sendUsage(sender, "/tempban <player> [-s] [-S] [-p] [-I] [server:scope] <duration> [reason]");
            return true;
        }

        String targetName = remaining[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        // Check exempt
        if (isExempt(targetUUID, "ban", sender)) {
            MessageUtil.sendError(sender, "That player is exempt from being banned.");
            return true;
        }

        // Parse duration
        long duration = TimeUtil.parseDuration(remaining[1]);
        if (duration <= 0) {
            MessageUtil.sendError(sender, "Invalid duration. Use format like: 1d, 2h, 30m, 1w, permanent");
            return true;
        }

        // Check duration limits
        long maxDuration = plugin.getStaffManager().getMaxDuration(sender, PunishmentType.TEMPBAN);
        if (maxDuration > 0 && duration > maxDuration) {
            MessageUtil.sendError(sender, "Duration exceeds your maximum allowed tempban duration (" +
                TimeUtil.formatDuration(maxDuration) + ").");
            return true;
        }

        // Parse reason
        String reason = null;
        if (remaining.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < remaining.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(remaining[i]);
            }
            reason = sb.toString();
        }

        // Check cooldown
        if (!plugin.getStaffManager().hasCooldownBypass(sender)) {
            int cooldown = plugin.getStaffManager().getCooldownSeconds(sender, PunishmentType.TEMPBAN);
            if (cooldown > 0) {
                // Cooldown check handled by manager
            }
        }

        plugin.getPunishmentManager().punish(sender, targetUUID, targetName,
            PunishmentType.TEMPBAN, duration, reason, flags);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            completions.add("1d");
            completions.add("7d");
            completions.add("30d");
            completions.add("permanent");
        }
        return completions;
    }
}
