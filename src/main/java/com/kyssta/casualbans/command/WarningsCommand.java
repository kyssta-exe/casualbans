package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.Punishment;
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

public class WarningsCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Determine target
        UUID targetUUID;
        String targetName;

        if (args.length > 0) {
            // Staff checking someone else
            if (!checkPermission(sender, "casualbans.warnings")) return true;
            targetName = args[0];
            targetUUID = resolveUUID(targetName);
            if (targetUUID == null) {
                MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
                return true;
            }
        } else {
            // Self-check
            if (!checkPermission(sender, "casualbans.warnings.self")) return true;
            if (!(sender instanceof Player)) {
                MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
                return true;
            }
            targetUUID = ((Player) sender).getUniqueId();
            targetName = sender.getName();
        }

        List<Punishment> warnings = plugin.getPunishmentManager().getWarnings(
            targetUUID, plugin.getDefaultServerScope());

        if (warnings.isEmpty()) {
            MessageUtil.sendPrefix(sender, targetName + " has <green>no warnings</green>.");
            return true;
        }

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("warnings.header", "$player", targetName));

        for (int i = 0; i < warnings.size(); i++) {
            Punishment p = warnings.get(i);
            String duration = p.getDurationString();

            MessageUtil.send(sender, MessageUtil.getMessage("warnings.entry",
                "$reason", p.getReason() != null ? p.getReason() : "No reason",
                "$duration", duration));
        }

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("warnings.end"));
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
