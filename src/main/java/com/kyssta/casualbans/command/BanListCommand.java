package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class BanListCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    private static final int ENTRIES_PER_PAGE = 10;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.banlist")) return true;

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        List<Punishment> allBans = plugin.getPunishmentManager().getActivePunishments(
            com.kyssta.casualbans.model.PunishmentType.BAN, plugin.getDefaultServerScope());

        // Also include IP bans
        List<Punishment> ipBans = plugin.getPunishmentManager().getActivePunishments(
            com.kyssta.casualbans.model.PunishmentType.IPBAN, plugin.getDefaultServerScope());
        allBans.addAll(ipBans);

        int totalPages = (int) Math.ceil((double) allBans.size() / ENTRIES_PER_PAGE);
        if (totalPages < 1) totalPages = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, allBans.size());

        MessageUtil.sendPrefix(sender, "<gold>=== Active Bans <dark_gray>(Page " + page + "/" + totalPages + ")</dark_gray> ===");

        if (allBans.isEmpty()) {
            MessageUtil.send(sender, "<gray>No active bans.</gray>");
            return true;
        }

        for (int i = startIndex; i < endIndex; i++) {
            Punishment p = allBans.get(i);
            String duration = p.getDurationString();
            String date = TimeUtil.formatDate(p.getDateStart());

            MessageUtil.send(sender, String.format(
                "<gray>%d.</gray> <white>%s</white> <dark_gray>-</dark_gray> <gray>%s</gray> <dark_gray>|</dark_gray> <red>%s</red> <dark_gray>|</dark_gray> <gray>%s</gray>",
                i + 1,
                p.getName() != null ? p.getName() : "Unknown",
                p.getReason() != null ? p.getReason() : "No reason",
                duration,
                date
            ));
        }

        if (totalPages > 1) {
            MessageUtil.send(sender, "<gold>Use /banlist <page> to view more.</gold>");
        }

        MessageUtil.sendPrefix(sender, "<gold>=== Total: " + allBans.size() + " active ban(s) ===");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("1", "2", "3");
        }
        return List.of();
    }
}
