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

public class UnwarnCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender, "/unwarn <player> [reason]");
            return true;
        }

        CommandFlags.ParsedResult parsed = parseFlags(args);
        CommandFlags flags = parsed.flags();
        String[] remaining = parsed.args();

        if (remaining.length < 1) {
            sendUsage(sender, "/unwarn <player> [reason]");
            return true;
        }

        String targetName = remaining[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        // Check permission (own or general)
        boolean canUnwarnOwn = sender.hasPermission("casualbans.unwarn.own");
        boolean canUnwarnAll = sender.hasPermission("casualbans.unwarn");
        if (!canUnwarnOwn && !canUnwarnAll) {
            MessageUtil.sendError(sender, "You do not have permission to use this command.");
            return true;
        }

        // If only own, check if executor matches warning executor
        if (!canUnwarnAll && canUnwarnOwn) {
            List<com.kyssta.casualbans.model.Punishment> warnings =
                plugin.getPunishmentManager().getWarnings(targetUUID, plugin.getDefaultServerScope());
            if (!warnings.isEmpty()) {
                com.kyssta.casualbans.model.Punishment lastWarn = warnings.get(warnings.size() - 1);
                String executorId = (sender instanceof Player) ?
                    ((Player) sender).getUniqueId().toString() : "CONSOLE";
                if (!lastWarn.getExecutorUUID().toString().equals(executorId) &&
                    !"CONSOLE".equals(executorId)) {
                    MessageUtil.sendError(sender, "You can only remove your own warnings.");
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
            PunishmentType.UNWARN, reason, flags);

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
