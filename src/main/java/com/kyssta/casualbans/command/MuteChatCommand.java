package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.CommandFlags;
import com.kyssta.casualbans.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MuteChatCommand extends BaseCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!checkPermission(sender, "casualbans.mutechat")) return true;

        CommandFlags.ParsedResult parsed = parseFlags(args);

        boolean newState = !plugin.getChatManager().isMuteChatEnabled();
        plugin.getChatManager().setMuteChatEnabled(newState);

        if (newState) {
            String msg = plugin.getConfigManager().getMessage("mutechat.disabled",
                "<red>Chat has been disabled by $executor</red>")
                .replace("$executor", sender.getName());
            if (parsed.flags().isSilent()) {
                MessageUtil.broadcast(msg, "casualbans.notify.silent");
            } else {
                MessageUtil.broadcast(msg, "casualbans.notify.broadcast");
            }
        } else {
            String msg = plugin.getConfigManager().getMessage("mutechat.enabled",
                "<green>Chat has been enabled by $executor</green>")
                .replace("$executor", sender.getName());
            MessageUtil.broadcast(msg, "casualbans.notify.broadcast");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("-s");
        }
        return completions;
    }
}
