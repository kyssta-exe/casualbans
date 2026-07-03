package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffHistoryCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.staffhistory")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/staffhistory <player> [type] [limit]");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        // Parse optional type filter
        PunishmentType typeFilter = null;
        int limit = 10;
        int argIndex = 1;

        if (args.length > argIndex) {
            String typeArg = args[argIndex].toUpperCase();
            try {
                typeFilter = PunishmentType.valueOf(typeArg);
                argIndex++;
            } catch (IllegalArgumentException ignored) {}
        }

        if (args.length > argIndex) {
            try {
                limit = Integer.parseInt(args[argIndex]);
                if (limit < 1) limit = 1;
                if (limit > 100) limit = 100;
            } catch (NumberFormatException ignored) {}
        }

        List<Punishment> history;
        if (typeFilter != null) {
            history = plugin.getPunishmentManager().getStaffHistoryByType(targetUUID, typeFilter, limit);
        } else {
            history = plugin.getPunishmentManager().getStaffHistory(targetUUID, limit);
        }

        MessageUtil.sendPrefix(sender, "<gold>=== Staff History: <white>" + targetName + "</white> ===");
        if (typeFilter != null) {
            MessageUtil.send(sender, "<gray>Filtered by type: " + typeFilter.getPastTense() + "</gray>");
        }

        if (history.isEmpty()) {
            MessageUtil.send(sender, "<gray>No staff history found for this player.</gray>");
            return true;
        }

        for (Punishment p : history) {
            String date = TimeUtil.formatDate(p.getDateStart());
            String duration = p.getOriginalDurationString();
            String target = p.getName() != null ? p.getName() : p.getUuid().toString().substring(0, 8);

            MessageUtil.send(sender, String.format(
                "<gray>[</gray>%s<gray>]</gray> <gold>%s</gold> <dark_gray>→</dark_gray> <white>%s</white> <dark_gray>|</dark_gray> <gray>%s</gray> <dark_gray>|</dark_gray> <gray>%s</gray>",
                date,
                p.getType().getPastTense(),
                target,
                p.getReason() != null ? p.getReason() : "No reason",
                duration
            ));
        }

        MessageUtil.sendPrefix(sender, "<gold>=== End of Staff History (" + history.size() + " entries) ===");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            for (PunishmentType type : PunishmentType.values()) {
                completions.add(type.name().toLowerCase());
            }
        }
        return completions;
    }
}
