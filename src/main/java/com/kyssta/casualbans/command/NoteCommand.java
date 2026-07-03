package com.kyssta.casualbans.command;

import com.kyssta.casualbans.model.StaffNote;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NoteCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "note":
                return handleNote(sender, args);
            case "notelist":
                return handleNoteList(sender, args);
            case "delnote":
                return handleDelNote(sender, args);
            default:
                return false;
        }
    }

    private boolean handleNote(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "casualbans.note")) return true;

        if (args.length == 0) {
            sendUsage(sender, "/note <player> [note]");
            MessageUtil.send(sender, MessageUtil.getMessage("notes.add"));
            return true;
        }

        String targetName = args[0];
        UUID targetUUID = resolveUUID(targetName);
        if (targetUUID == null) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.no-player"));
            return true;
        }

        // /note <player> — list notes
        if (args.length == 1) {
            List<StaffNote> notes = plugin.getStorageProvider().getNotes(targetUUID);

            if (notes.isEmpty()) {
                MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.header", "$player", targetName));
                MessageUtil.send(sender, MessageUtil.getMessage("notes.none"));
                return true;
            }

            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.header", "$player", targetName));
            for (StaffNote note : notes) {
                String date = TimeUtil.formatDate(note.getTimestamp());
                MessageUtil.send(sender, MessageUtil.getMessage("notes.entry",
                    "$id", String.valueOf(note.getId()),
                    "$date", date,
                    "$author", note.getAuthorName(),
                    "$note", note.getNote()));
            }
            return true;
        }

        // /note <player> <note> — add a note
        StringBuilder noteText = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (noteText.length() > 0) noteText.append(" ");
            noteText.append(args[i]);
        }

        String authorName = sender.getName();
        UUID authorUUID;
        if (sender instanceof Player) {
            authorUUID = ((Player) sender).getUniqueId();
        } else {
            authorUUID = UUID.nameUUIDFromBytes("Console".getBytes());
        }

        StaffNote note = StaffNote.builder()
                .targetUUID(targetUUID)
                .targetName(targetName)
                .note(noteText.toString())
                .authorUUID(authorUUID)
                .authorName(authorName)
                .timestamp(System.currentTimeMillis())
                .build();

        plugin.getStorageProvider().saveNote(note);

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.added"));
        return true;
    }

    private boolean handleNoteList(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "casualbans.note")) return true;

        List<StaffNote> allNotes = plugin.getStorageProvider().getAllNotes();

        if (allNotes.isEmpty()) {
            MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.all-header"));
            MessageUtil.send(sender, MessageUtil.getMessage("notes.none"));
            return true;
        }

        // Optional player filter
        if (args.length > 0) {
            String filterName = args[0];
            UUID filterUUID = resolveUUID(filterName);
            if (filterUUID != null) {
                allNotes = allNotes.stream()
                        .filter(n -> n.getTargetUUID().equals(filterUUID))
                        .collect(Collectors.toList());
            }
        }

        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.all-header"));
        for (StaffNote note : allNotes) {
            String date = TimeUtil.formatDate(note.getTimestamp());
            MessageUtil.send(sender, MessageUtil.getMessage("notes.entry",
                "$id", String.valueOf(note.getId()),
                "$date", date,
                "$author", note.getAuthorName(),
                "$note", note.getNote()));
        }
        MessageUtil.send(sender, MessageUtil.getMessage("notes.total", "$count", String.valueOf(allNotes.size())));
        return true;
    }

    private boolean handleDelNote(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "casualbans.note.delete")) return true;

        if (args.length < 1) {
            sendUsage(sender, "/delnote <id>");
            return true;
        }

        long noteId;
        try {
            noteId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Invalid note ID: " + args[0]);
            return true;
        }

        plugin.getStorageProvider().deleteNote(noteId);
        MessageUtil.sendPrefix(sender, MessageUtil.getMessage("notes.deleted"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("note") && args.length == 1) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (cmdName.equals("notelist") && args.length == 1) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}
