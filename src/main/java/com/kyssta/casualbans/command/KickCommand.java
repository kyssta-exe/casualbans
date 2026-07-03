package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.CommandFlags;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KickCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.kick")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/kick <player> [-s] [-S] [-p] [reason]");
            return true;
        }

        CommandFlags.ParsedResult parsed = parseFlags(args);
        CommandFlags flags = parsed.flags();
        String[] remaining = parsed.args();

        if (remaining.length < 1) {
            sendUsage(sender, "/kick <player> [-s] [-S] [-p] [reason]");
            return true;
        }

        String targetName = remaining[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        Player target = getPlayer(targetName);
        if (target == null) {
            MessageUtil.sendError(sender, "That player is not online.");
            return true;
        }

        // Check exempt
        if (isExempt(targetUUID, "kick", sender)) {
            MessageUtil.sendError(sender, "That player is exempt from being kicked.");
            return true;
        }

        // Parse reason
        String reason = null;
        if (remaining.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < remaining.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(remaining[i]);
            }
            reason = sb.toString();
        }

        // Check cooldown
        if (!plugin.getStaffManager().hasCooldownBypass(sender)) {
            int cooldown = plugin.getStaffManager().getCooldownSeconds(sender, PunishmentType.KICK);
            if (cooldown > 0) {
                // Cooldown check handled by manager
            }
        }

        plugin.getPunishmentManager().punish(sender, targetUUID, targetName,
            PunishmentType.KICK, 0, reason, flags);

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
