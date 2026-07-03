package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Parsed command flags for punishment commands.
 * Supports: -s (silent), -S (extra silent), -p (public),
 * -I (IP), -g (global scope), -N (no override),
 * -d (delete existing), -m (modify in-place),
 * server:scope, --sender, --confirm, --hide, --skip, --no-queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandFlags {
    private boolean silent;
    private boolean extraSilent;
    private boolean publicPunish;
    private boolean ipPunish;
    private boolean global;
    private boolean noOverride;
    private boolean delete;
    private boolean modify;
    private boolean hide;
    private boolean skip;
    private boolean noQueue;
    private boolean confirm;
    private String customSender;
    private String serverScope;
    private String templateName;

    /**
     * Parse flags from command arguments.
     * Returns the remaining non-flag arguments.
     */
    public static ParsedResult parse(String[] args) {
        CommandFlags flags = new CommandFlags();
        List<String> remaining = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // -- is end of flags marker
            if (arg.equals("--")) {
                for (int j = i + 1; j < args.length; j++) remaining.add(args[j]);
                break;
            }

            if (arg.startsWith("--")) {
                // Long flags
                switch (arg.toLowerCase()) {
                    case "--sender": if (i + 1 < args.length) flags.customSender = args[++i]; break;
                    case "--confirm": flags.confirm = true; break;
                    case "--hide": flags.hide = true; break;
                    case "--skip": flags.skip = true; break;
                    case "--no-queue": flags.noQueue = true; break;
                    default: remaining.add(arg); break;
                }
            } else if (arg.startsWith("-") && !arg.startsWith("--") && arg.length() > 1) {
                // Short flags (-s, -S, -p, -I, -g, -N, -d, -m)
                String flagStr = arg.substring(1);
                for (char c : flagStr.toCharArray()) {
                    switch (c) {
                        case 's': flags.silent = true; break;
                        case 'S': if (!flags.silent) flags.extraSilent = true; break;
                        case 'p': flags.publicPunish = true; break;
                        case 'I': flags.ipPunish = true; break;
                        case 'g': flags.global = true; break;
                        case 'N': flags.noOverride = true; break;
                        case 'd': flags.delete = true; break;
                        case 'm': flags.modify = true; break;
                        default: break;
                    }
                }
            } else if (arg.toLowerCase().startsWith("server:")) {
                flags.serverScope = arg.substring(7);
            } else {
                remaining.add(arg);
            }
        }

        return new ParsedResult(flags, remaining.toArray(new String[0]));
    }

    public record ParsedResult(CommandFlags flags, String[] args) {}
}
