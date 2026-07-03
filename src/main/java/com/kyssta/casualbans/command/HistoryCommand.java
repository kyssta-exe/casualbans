package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HistoryCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.history")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/history <player> [type] [limit]");
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
            } catch (IllegalArgumentException ignored) {
                // Not a type, check if it's a number
            }
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
            history = plugin.getPunishmentManager().getHistoryByType(targetUUID, typeFilter, limit);
        } else {
            history = plugin.getPunishmentManager().getHistory(targetUUID, limit);
        }

        String displayName = history.isEmpty() ? targetName :
            history.get(0).getName() != null ? history.get(0).getName() : targetName;

        MessageUtil.sendPrefix(sender, "<gold>=== Punishment History: <white>" + displayName + "</white> ===");
        if (typeFilter != null) {
            MessageUtil.send(sender, "<gray>Filtered by type: " + typeFilter.getPastTense() + "</gray>");
        }

        if (history.isEmpty()) {
            MessageUtil.send(sender, "<gray>No punishment history found.</gray>");
            return true;
        }

        for (Punishment p : history) {
            String status = p.isActive() ? "<red>Active</red>" :
                p.isExpired() ? "<gray>Expired</gray>" : "<green>Inactive</green>";
            String duration = p.getOriginalDurationString();
            String date = TimeUtil.formatDate(p.getDateStart());

            MessageUtil.send(sender, String.format(
                "<gray>[</gray>%s<gray>]</gray> <gold>%s</gold> <dark_gray>-</dark_gray> <white>%s</white> <dark_gray>|</dark_gray> <gray>%s</gray> <dark_gray>|</dark_gray> <gray>%s</gray> <dark_gray>|</dark_gray> %s",
                date,
                p.getType().getPastTense(),
                p.getReason() != null ? p.getReason() : "No reason",
                p.getExecutorName() != null ? p.getExecutorName() : "Console",
                duration,
                status
            ));
        }

        MessageUtil.sendPrefix(sender, "<gold>=== End of History (" + history.size() + " entries) ===");
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
