package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PruneHistoryCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.prunehistory")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/prunehistory <player> [duration]");
            return true;
        }

        var uuid = resolveUUID(args[0]);
        if (uuid == null) {
            MessageUtil.sendError(sender, "Player not found.");
            return true;
        }

        long olderThan = 0;
        if (args.length >= 2) {
            olderThan = com.kyssta.casualbans.util.TimeUtil.parseDuration(args[1]);
        }

        int count = plugin.getStorageProvider().pruneHistory(uuid, olderThan);
        MessageUtil.sendSuccess(sender, "History pruned. " + count + " entries removed.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        return args.length == 1 ? null : new ArrayList<>();
    }
}
