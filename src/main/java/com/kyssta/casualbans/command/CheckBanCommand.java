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
import java.util.UUID;

public class CheckBanCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.checkban")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/checkban <player>");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
            return true;
        }

        Punishment activeBan = plugin.getPunishmentManager().getActiveBan(
            targetUUID, plugin.getDefaultServerScope());

        if (activeBan == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("checkban.status-not-banned"));
            return true;
        }

        String duration = activeBan.getDurationString();
        String date = TimeUtil.formatDate(activeBan.getDateStart());
        String expires = activeBan.isPermanent() ? "<red>Never</red>" :
            TimeUtil.formatDate(activeBan.getDateEnd());
        String reason = activeBan.getReason() != null ? activeBan.getReason() : "No reason";
        String executor = activeBan.getExecutorName() != null ? activeBan.getExecutorName() : "Console";

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("checkban.header", "$player", targetName));
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.status-banned"));
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.reason", "$reason", reason));
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.banned-by", "$executor", executor));
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.date", "$date", date));
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.duration", "$duration", duration));
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.expires", "$expires", expires));
        if (activeBan.getServerScope() != null && !activeBan.getServerScope().equals("*")) {
            MessageUtil.send(sender, MessageUtil.getMessage("checkban.scope", "$scope", activeBan.getServerScope()));
        }
        MessageUtil.send(sender, MessageUtil.getMessage("checkban.id", "$id", String.valueOf(activeBan.getId())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
