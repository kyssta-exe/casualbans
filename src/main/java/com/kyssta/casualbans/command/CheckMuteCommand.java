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

public class CheckMuteCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.checkmute")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/checkmute <player>");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
            return true;
        }

        Punishment activeMute = plugin.getPunishmentManager().getActiveMute(
            targetUUID, plugin.getDefaultServerScope());

        if (activeMute == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("checkmute.status-not-muted"));
            return true;
        }

        String duration = activeMute.getDurationString();
        String date = TimeUtil.formatDate(activeMute.getDateStart());
        String expires = activeMute.isPermanent() ? "<red>Never</red>" :
            TimeUtil.formatDate(activeMute.getDateEnd());
        String reason = activeMute.getReason() != null ? activeMute.getReason() : "No reason";
        String executor = activeMute.getExecutorName() != null ? activeMute.getExecutorName() : "Console";

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("checkmute.header", "$player", targetName));
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.status-muted"));
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.reason", "$reason", reason));
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.muted-by", "$executor", executor));
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.date", "$date", date));
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.duration", "$duration", duration));
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.expires", "$expires", expires));
        if (activeMute.getServerScope() != null && !activeMute.getServerScope().equals("*")) {
            MessageUtil.send(sender, MessageUtil.getMessage("checkmute.scope", "$scope", activeMute.getServerScope()));
        }
        MessageUtil.send(sender, MessageUtil.getMessage("checkmute.id", "$id", String.valueOf(activeMute.getId())));
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
