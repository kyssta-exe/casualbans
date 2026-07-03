package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StaffRollbackCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.staffrollback")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/staffrollback <player> [duration]");
            return true;
        }

        boolean consoleOnly = plugin.getConfig().getBoolean("security.staffrollback", true);
        if (consoleOnly && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            MessageUtil.sendError(sender, "This command can only be used from console.");
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

        int count = plugin.getStorageProvider().rollbackStaff(uuid, olderThan);
        MessageUtil.sendSuccess(sender, "Rollback complete. " + count + " entries removed.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        return args.length == 1 ? null : new ArrayList<>();
    }
}
