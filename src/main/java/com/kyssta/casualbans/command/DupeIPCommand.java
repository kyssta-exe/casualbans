package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.UUIDUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DupeIPCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.dupeip")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/dupeip <player>");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendError(sender, "Player not found: " + targetName);
            return true;
        }

        // Get IPs for this player
        List<String> ips = plugin.getInvestigationManager().getIPsByUUID(targetUUID);

        if (ips.isEmpty()) {
            MessageUtil.sendPrefix(sender, "<gold>No IP data recorded for <white>" + targetName + "</white>.</gold>");
            return true;
        }

        MessageUtil.sendPrefix(sender, "<gold>=== Alt Accounts for <white>" + targetName + "</white> ===");
        MessageUtil.send(sender, "<dark_gray>Found " + ips.size() + " IP(s) on record.</dark_gray>");

        java.util.Set<UUID> allAccounts = new java.util.HashSet<>();
        for (String ip : ips) {
            List<UUID> accounts = plugin.getInvestigationManager().getAccountsByIP(ip);
            for (UUID account : accounts) {
                if (!account.equals(targetUUID)) {
                    allAccounts.add(account);
                }
            }
        }

        if (allAccounts.isEmpty()) {
            MessageUtil.send(sender, "<gray>No alt accounts found.</gray>");
            return true;
        }

        MessageUtil.send(sender, "<gold>Possible alt accounts (" + allAccounts.size() + "):</gold>");
        int count = 0;
        for (UUID altUUID : allAccounts) {
            String altName = UUIDUtil.getName(altUUID);
            count++;
            MessageUtil.send(sender, String.format(
                "<gray>%d.</gray> <white>%s</white> <dark_gray>-</dark_gray> <gray>%s</gray>",
                count, altName, altUUID.toString().substring(0, 8)
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
