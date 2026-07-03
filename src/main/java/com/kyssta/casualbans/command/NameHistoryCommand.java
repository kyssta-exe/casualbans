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

public class NameHistoryCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.namehistory")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/namehistory <player>");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
            return true;
        }

        List<StorageProvider.NameRecord> nameHistory = plugin.getInvestigationManager().getNameHistory(targetUUID);

        if (nameHistory.isEmpty()) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("namehistory.none", "$player", targetName));
            return true;
        }

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("namehistory.header", "$player", targetName));
        MessageUtil.send(sender, MessageUtil.getMessage("namehistory.count", "$count", String.valueOf(nameHistory.size())));

        for (int i = nameHistory.size() - 1; i >= 0; i--) {
            StorageProvider.NameRecord record = nameHistory.get(i);
            String date = TimeUtil.formatDate(record.timestamp());

            MessageUtil.send(sender, MessageUtil.getMessage("namehistory.entry",
                "$date", date,
                "$name", record.newName()));
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
