package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ClearChatCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.clearchat")) return true;

        boolean bypassAllowed = plugin.getConfig().getBoolean("allow.clearchat-bypass", false);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (bypassAllowed && player.hasPermission("casualbans.clearchat.bypass")) {
                MessageUtil.send(player, "<dark_gray>Chat cleared by " + sender.getName()
                    + ". You can still see this due to your bypass permission.</dark_gray>");
                continue;
            }
            for (int i = 0; i < 150; i++) {
                player.sendMessage("");
            }
        }

        String msg = plugin.getConfigManager().getMessage("clearchat.broadcast",
            "<green>Chat has been cleared by $executor.</green>")
            .replace("$executor", sender.getName());
        MessageUtil.broadcast(msg, "casualbans.clearchat.bypass");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        return new ArrayList<>();
    }
}
