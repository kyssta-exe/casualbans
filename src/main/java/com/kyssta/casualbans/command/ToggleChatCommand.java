package com.kyssta.casualbans.command;

import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ToggleChatCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.togglechat")) return true;

        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "Only players can toggle their chat.");
            return true;
        }

        boolean isToggled = plugin.getChatManager().isChatToggledOff(player.getUniqueId());
        plugin.getChatManager().setChatToggled(player.getUniqueId(), !isToggled);

        if (!isToggled) {
            MessageUtil.sendPrefix(player, "<red>Your chat has been toggled off.</red>");
        } else {
            MessageUtil.sendPrefix(player, "<green>Your chat has been toggled on.</green>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        return new ArrayList<>();
    }
}
