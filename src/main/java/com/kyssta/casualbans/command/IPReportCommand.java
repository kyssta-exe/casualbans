package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.UUIDUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Scans all online players and reports any IP address sharing.
 */
public class IPReportCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.ipreport")) return true;

        MessageUtil.sendPrefix(sender, "<gold>Scanning online players for shared IPs...</gold>");

        Collection<? extends Player> onlinePlayers = plugin.getServer().getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            MessageUtil.send(sender, "<gray>No players online to scan.</gray>");
            return true;
        }

        // Build IP → Players map
        Map<String, List<Player>> ipMap = new HashMap<>();
        for (Player player : onlinePlayers) {
            String ip = player.getAddress() != null ?
                player.getAddress().getAddress().getHostAddress() : null;
            if (ip == null) continue;

            ipMap.computeIfAbsent(ip, k -> new ArrayList<>()).add(player);
        }

        // Filter to only IPs with multiple players
        Map<String, List<Player>> sharedIPs = new HashMap<>();
        for (Map.Entry<String, List<Player>> entry : ipMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                sharedIPs.put(entry.getKey(), entry.getValue());
            }
        }

        if (sharedIPs.isEmpty()) {
            MessageUtil.sendSuccess(sender, "No IP sharing detected among " + onlinePlayers.size() + " online players.");
            return true;
        }

        MessageUtil.sendPrefix(sender, "<red>=== IP Sharing Detected (" + sharedIPs.size() + " shared IPs) ===");

        for (Map.Entry<String, List<Player>> entry : sharedIPs.entrySet()) {
            StringBuilder players = new StringBuilder();
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (i > 0) players.append("<dark_gray>, </dark_gray>");
                players.append("<white>").append(entry.getValue().get(i).getName()).append("</white>");
            }

            // Also check if this IP is banned
            boolean ipBanned = plugin.getPunishmentManager().isBanned(
                UUID.nameUUIDFromBytes(entry.getKey().getBytes()),
                plugin.getDefaultServerScope()
            );

            MessageUtil.send(sender, String.format(
                "<gold>IP:</gold> <yellow>%s</yellow> %s<dark_gray>|</dark_gray> %s <dark_gray>|</dark_gray> %s",
                entry.getKey(),
                ipBanned ? "<red>[BANNED]</red>" : "<green>[CLEAN]</green>",
                players.toString(),
                "<gray>(" + entry.getValue().size() + " players)</gray>"
            ));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
