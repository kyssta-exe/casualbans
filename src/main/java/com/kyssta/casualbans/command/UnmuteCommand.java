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

public class UnmuteCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender, "/unmute <player> [reason]");
            return true;
        }

        CommandFlags.ParsedResult parsed = parseFlags(args);
        CommandFlags flags = parsed.flags();
        String[] remaining = parsed.args();

        if (remaining.length < 1) {
            sendUsage(sender, "/unmute <player> [reason]");
            return true;
        }

        String targetName = remaining[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        // Check permission (own or general)
        boolean canUnmuteOwn = sender.hasPermission("casualbans.unmute.own");
        boolean canUnmuteAll = sender.hasPermission("casualbans.unmute");
        if (!canUnmuteOwn && !canUnmuteAll) {
            MessageUtil.sendError(sender, "You do not have permission to use this command.");
            return true;
        }

        // If only own, check if executor matches punishment executor
        if (!canUnmuteAll && canUnmuteOwn) {
            com.kyssta.casualbans.model.Punishment activeMute =
                plugin.getPunishmentManager().getActiveMute(targetUUID, plugin.getDefaultServerScope());
            if (activeMute != null) {
                String executorId = (sender instanceof Player) ?
                    ((Player) sender).getUniqueId().toString() : "CONSOLE";
                if (!activeMute.getExecutorUUID().toString().equals(executorId) &&
                    !"CONSOLE".equals(executorId)) {
                    MessageUtil.sendError(sender, "You can only unmute your own punishments.");
                    return true;
                }
            }
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

        plugin.getPunishmentManager().removePunishment(sender, targetUUID,
            PunishmentType.UNMUTE, reason, flags);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<com.kyssta.casualbans.model.Punishment> mutes =
                plugin.getPunishmentManager().getActivePunishments(
                    PunishmentType.MUTE, plugin.getDefaultServerScope());
            for (com.kyssta.casualbans.model.Punishment p : mutes) {
                completions.add(p.getName());
            }
        }
        return completions;
    }
}
