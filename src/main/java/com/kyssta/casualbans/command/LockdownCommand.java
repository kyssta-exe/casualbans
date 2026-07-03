package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LockdownCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.lockdown")) return true;

        if (args.length >= 1 && args[0].equalsIgnoreCase("end")) {
            plugin.getLockdownManager().endLockdown();
            String msg = plugin.getConfigManager().getMessage("lockdown.deactivated",
                "<green>Server lockdown deactivated.</green>");
            MessageUtil.sendPrefix(sender, msg);
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, "/lockdown [reason|end]");
            return true;
        }

        String reason = String.join(" ", args);
        plugin.getLockdownManager().setLockdown(reason);

        // Kick all non-exempt players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.hasPermission("casualbans.lockdown.bypass") && !player.isOp()) {
                String kickMsg = plugin.getConfigManager().getMessage("lockdown.kick-message",
                    "<red>Server lockdown is active. Try again later.</red>\n<gray>Reason:</gray> $reason")
                    .replace("$reason", reason);
                player.kickPlayer(MessageUtil.legacy(kickMsg));
            }
        }

        String activated = plugin.getConfigManager().getMessage("lockdown.activated",
            "<red>Server lockdown activated: $reason</red>")
            .replace("$reason", reason);
        MessageUtil.broadcast(activated, "casualbans.notify");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("end");
        }
        return completions;
    }
}
