package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GeoIP lookup for players or raw IP addresses.
 */
public class GeoIPCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "casualbans.geoip")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/geoip <player|IP>");
            return true;
        }

        String input = args[0];

        // Try as player first
        Player target = getPlayer(input);
        if (target != null) {
            String ip = target.getAddress() != null ?
                target.getAddress().getAddress().getHostAddress() : null;
            if (ip == null) {
                MessageUtil.sendError(sender, "Cannot determine IP for that player.");
                return true;
            }

            String playerName = target.getName();

            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("geoip.header", "$player", playerName));
            MessageUtil.send(sender, MessageUtil.getMessage("geoip.ip", "$ip", ip));

            String country = plugin.getGeoIPManager().getCountry(ip);
            if (country != null && !country.equals("Unknown")) {
                MessageUtil.send(sender, MessageUtil.getMessage("geoip.country", "$country", country));
            } else {
                MessageUtil.send(sender, MessageUtil.getMessage("geoip.unknown"));
            }
            return true;
        }

        // Try as UUID
        UUID uuid = resolveUUID(input);
        if (uuid != null) {
            Player offlineTarget = Bukkit.getPlayer(uuid);
            if (offlineTarget != null) {
                String ip = offlineTarget.getAddress() != null ?
                    offlineTarget.getAddress().getAddress().getHostAddress() : null;
                if (ip != null) {
                    String country = plugin.getGeoIPManager().getCountry(ip);
                    String name = offlineTarget.getName();

                    MessageUtil.sendPrefix(sender, MessageUtil.getMessage("geoip.header", "$player", name));
                    MessageUtil.send(sender, MessageUtil.getMessage("geoip.ip", "$ip", ip));
                    MessageUtil.send(sender, MessageUtil.getMessage("geoip.country", "$country",
                        country != null ? country : "Unknown"));
                    return true;
                }
            }
        }

        // Try as raw IP
        String ip = input;
        // Basic IP validation
        if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("player-not-found"));
            return true;
        }

        String country = plugin.getGeoIPManager().getCountry(ip);
        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("geoip.header", "$player", ip));
        MessageUtil.send(sender, MessageUtil.getMessage("geoip.country", "$country",
            country != null ? country : "Unknown"));
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
