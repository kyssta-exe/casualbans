package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
// Statistics API removed in Paper 1.21+

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LastSessionCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.lastsession")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/lastsession <player>");
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
            return true;
        }

        Player onlineTarget = getPlayer(targetName);

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("lastsession.header", "$player", targetName));

        if (onlineTarget != null) {
            MessageUtil.send(sender, MessageUtil.getMessage("lastsession.online"));
            long sessionLength = System.currentTimeMillis() - onlineTarget.getLastLogin();
            if (sessionLength > 0) {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.session-length", "$duration",
                    TimeUtil.formatDuration(sessionLength)));
            }
            String ip = onlineTarget.getAddress() != null ?
                onlineTarget.getAddress().getAddress().getHostAddress() : "Unknown";
            MessageUtil.send(sender, MessageUtil.getMessage("lastsession.last-ip", "$ip", ip));
            MessageUtil.send(sender, MessageUtil.getMessage("lastsession.world", "$world", onlineTarget.getWorld().getName()));
            MessageUtil.send(sender, MessageUtil.getMessage("lastsession.gamemode", "$mode", onlineTarget.getGameMode().name()));
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUUID);
            if (!offline.hasPlayedBefore()) {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.never-joined"));
                return true;
            }

            long lastLogin = offline.getLastLogin();
            long lastSeen = offline.getLastSeen();

            if (lastLogin > 0) {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.last-login", "$date",
                    TimeUtil.formatDate(lastLogin)));
            } else {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.last-login", "$date", "Unknown"));
            }

            if (lastSeen > 0) {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.last-seen", "$date",
                    TimeUtil.formatDate(lastSeen)));
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.last-seen-ago", "$duration",
                    TimeUtil.formatDuration(System.currentTimeMillis() - lastSeen)));
            }

            // Try to get player IP from investigation manager
            List<String> ips = plugin.getInvestigationManager().getIPsByUUID(targetUUID);
            if (!ips.isEmpty()) {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.last-ip", "$ip", ips.get(ips.size() - 1)));
            }
        }

        // First played
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUUID);
        if (offline.hasPlayedBefore()) {
            long firstPlayed = offline.getFirstPlayed();
            if (firstPlayed > 0) {
                MessageUtil.send(sender, MessageUtil.getMessage("lastsession.first-joined", "$date",
                    TimeUtil.formatDate(firstPlayed)));
            }
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
