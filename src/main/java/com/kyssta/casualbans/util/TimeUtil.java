package com.kyssta.casualbans.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(\\d+)\\s*(s(?:ec(?:ond)?s?)?|m(?:in(?:ute)?s?)?|h(?:ou)?r?s?|d(?:ay)?s?|w(?:ee)?k?s?|mo(?:nth)?s?|y(?:ea)?r?s?)",
        Pattern.CASE_INSENSITIVE
    );

    private TimeUtil() {}

    public static long parseDuration(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("permanent")) {
            return -1;
        }

        long total = 0;
        Matcher matcher = DURATION_PATTERN.matcher(input);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();

            switch (unit.charAt(0)) {
                case 's': total += value * 1000; break;
                case 'm':
                    if (unit.startsWith("mo")) total += value * 2592000000L;
                    else total += value * 60000;
                    break;
                case 'h': total += value * 3600000; break;
                case 'd': total += value * 86400000; break;
                case 'w': total += value * 604800000; break;
                case 'y': total += value * 31536000000L; break;
            }
        }
        return total;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "permanent";

        long totalSeconds = millis / 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long totalDays = totalHours / 24;
        long days = totalDays % 7;
        long weeks = totalDays / 7;
        long months = days / 30;
        long years = days / 365;

        StringBuilder sb = new StringBuilder();

        if (years > 0) {
            sb.append(years).append(years == 1 ? " year" : " years");
            days %= 365;
        }

        // Only show months if less than a year
        if (years == 0) {
            months = days / 30;
            if (months > 0) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(months).append(months == 1 ? " month" : " months");
                days -= months * 30;
            }
        }

        if (days > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(days).append(days == 1 ? " day" : " days");
        }

        if (hours > 0 && weeks == 0 && months == 0 && years == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }

        if (minutes > 0 && days == 0 && months == 0 && years == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        if (seconds > 0 && hours == 0 && days == 0 && months == 0 && years == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString().isEmpty() ? "0 seconds" : sb.toString();
    }

    public static String formatDate(long millis) {
        if (millis <= 0) return "N/A";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }

    public static String formatDateShort(long millis) {
        if (millis <= 0) return "N/A";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date(millis));
    }
}
