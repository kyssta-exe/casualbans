package com.kyssta.casualbans.command;

import com.kyssta.casualbans.storage.StorageProvider;
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

public class IPHistoryCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.iphistory")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/iphistory <player> [limit]");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                if (limit < 1) limit = 1;
                if (limit > 50) limit = 50;
            } catch (NumberFormatException ignored) {}
        }

        List<StorageProvider.IPRecord> ipHistory = plugin.getInvestigationManager().getIPHistory(targetUUID);

        if (ipHistory.isEmpty()) {
            MessageUtil.sendPrefix(sender, "<gold>No IP history recorded for <white>" + targetName + "</white>.</gold>");
            return true;
        }

        MessageUtil.sendPrefix(sender, "<gold>=== IP History for <white>" + targetName + "</white> ===");
        MessageUtil.send(sender, "<dark_gray>" + ipHistory.size() + " IP address(es) on record (showing last " + limit + ")</dark_gray>");

        int count = 0;
        for (int i = ipHistory.size() - 1; i >= 0 && count < limit; i--, count++) {
            StorageProvider.IPRecord record = ipHistory.get(i);
            String date = TimeUtil.formatDate(record.timestamp());

            MessageUtil.send(sender, String.format(
                "<gray>%d.</gray> <yellow>%s</yellow> <dark_gray>-</dark_gray> <gray>%s</gray>",
                count + 1,
                record.ip(),
                date
            ));
        }

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
