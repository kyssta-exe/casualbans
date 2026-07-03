package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Punishment {
    private long id;
    private PunishmentType type;
    private UUID uuid;
    private String ip;
    private String name;
    private String reason;
    private UUID executorUUID;
    private String executorName;
    private UUID removedByUUID;
    private String removedByName;
    private String removalReason;
    private long dateStart;
    private long dateEnd;
    private String serverScope;
    private String serverOrigin;
    private boolean silent;
    private boolean ipPunish;
    private boolean active;
    private String templateName;
    private long duration;

    public boolean isExpired() {
        return dateEnd > 0 && System.currentTimeMillis() > dateEnd;
    }

    public boolean isPermanent() {
        return dateEnd <= 0 && (type == PunishmentType.BAN || type == PunishmentType.IPBAN
            || type == PunishmentType.MUTE || type == PunishmentType.IPMUTE);
    }

    public long getRemainingDuration() {
        if (isPermanent() || dateEnd <= 0) return -1;
        return Math.max(0, dateEnd - System.currentTimeMillis());
    }

    public String getDurationString() {
        if (isPermanent()) return "permanent";
        if (isExpired()) return "expired";
        long remaining = getRemainingDuration();
        if (remaining <= 0) return "expired";
        return formatDuration(remaining);
    }

    public String getOriginalDurationString() {
        if (duration <= 0) return "permanent";
        return formatDuration(duration);
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "permanent";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        StringBuilder sb = new StringBuilder();
        if (years > 0) { sb.append(years).append(years == 1 ? " year " : " years "); days %= 365; }
        if (months > 0 && years == 0) { sb.append(months).append(months == 1 ? " month " : " months "); days %= 30; }
        if (weeks > 0 && months == 0) { sb.append(weeks).append(weeks == 1 ? " week " : " weeks "); days %= 7; }
        if (days > 0 && weeks == 0) { sb.append(days).append(days == 1 ? " day " : " days "); }
        if (hours > 0 && days == 0) { sb.append(hours).append(hours == 1 ? " hour " : " hours "); }
        if (minutes > 0 && hours == 0) { sb.append(minutes).append(minutes == 1 ? " minute " : " minutes "); }
        if (seconds > 0 && minutes == 0 && days == 0) { sb.append(seconds).append(seconds == 1 ? " second " : " seconds "); }

        return sb.toString().trim();
    }

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("permanent")) return -1;

        // Support formats: 1d, 2h, 30m, 1w, 1mo, 1y, 1d 2h, etc.
        long total = 0;
        String[] parts = input.split("\\s+");
        for (String part : parts) {
            part = part.trim().toLowerCase();
            if (part.isEmpty()) continue;
            char unit = part.charAt(part.length() - 1);
            String numStr = part.substring(0, part.length() - 1);
            long num;
            try {
                num = Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                // Try parsing as plain number (seconds)
                try {
                    num = Long.parseLong(part);
                    unit = 's';
                } catch (NumberFormatException ex) {
                    continue;
                }
            }
            switch (unit) {
                case 's': total += num * 1000L; break;
                case 'm': total += num * 60000L; break;
                case 'h': total += num * 3600000L; break;
                case 'd': total += num * 86400000L; break;
                case 'w': total += num * 604800000L; break;
                case 'o': total += num * 2592000000L; break; // month
                case 'y': total += num * 31536000000L; break;
                default: total += num * 1000L; break;
            }
        }
        return total;
    }
}
