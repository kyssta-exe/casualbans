package com.kyssta.casualbans;

import com.kyssta.casualbans.command.*;
import org.bukkit.command.PluginCommand;

/**
 * Registers all command executors and tab completers.
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    public static void registerCommands(CasualBans plugin) {
        // Punishment commands
        register(plugin, "ban", new BanCommand());
        register(plugin, "tempban", new TempBanCommand());
        register(plugin, "ipban", new IPBanCommand());
        register(plugin, "mute", new MuteCommand());
        register(plugin, "tempmute", new TempMuteCommand());
        register(plugin, "ipmute", new IPMuteCommand());
        register(plugin, "warn", new WarnCommand());
        register(plugin, "kick", new KickCommand());
        register(plugin, "unban", new UnbanCommand());
        register(plugin, "unmute", new UnmuteCommand());
        register(plugin, "unwarn", new UnwarnCommand());

        // History & Investigation
        register(plugin, "history", new HistoryCommand());
        register(plugin, "staffhistory", new StaffHistoryCommand());
        register(plugin, "checkban", new CheckBanCommand());
        register(plugin, "checkmute", new CheckMuteCommand());
        register(plugin, "banlist", new BanListCommand());
        register(plugin, "mutelist", new MuteListCommand());
        register(plugin, "warnings", new WarningsCommand());
        register(plugin, "dupeip", new DupeIPCommand());
        register(plugin, "ipreport", new IPReportCommand());
        register(plugin, "iphistory", new IPHistoryCommand());
        register(plugin, "namehistory", new NameHistoryCommand());
        register(plugin, "lastsession", new LastSessionCommand());
        register(plugin, "geoip", new GeoIPCommand());

        // Staff tools
        register(plugin, "staffrollback", new StaffRollbackCommand());
        register(plugin, "prunehistory", new PruneHistoryCommand());
        register(plugin, "lockdown", new LockdownCommand());
        register(plugin, "mutechat", new MuteChatCommand());
        register(plugin, "togglechat", new ToggleChatCommand());
        register(plugin, "clearchat", new ClearChatCommand());

        // Staff notes
        NoteCommand noteCmd = new NoteCommand();
        register(plugin, "note", noteCmd);
        register(plugin, "notelist", noteCmd);
        register(plugin, "delnote", noteCmd);

        // Admin
        register(plugin, "casualbans", new CasualBansCommand());
        register(plugin, "backup", new BackupCommand());
    }

    private static void register(CasualBans plugin, String name, Object executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml");
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor ce) {
            cmd.setExecutor(ce);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tc) {
            cmd.setTabCompleter(tc);
        }
    }
}
