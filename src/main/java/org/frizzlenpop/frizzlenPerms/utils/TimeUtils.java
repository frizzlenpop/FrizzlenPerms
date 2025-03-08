package org.frizzlenpop.frizzlenPerms.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with time values and durations.
 */
public class TimeUtils {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(?:([0-9]+)d)?(?:([0-9]+)h)?(?:([0-9]+)m)?(?:([0-9]+)s)?");
    
    /**
     * Parses a time string in the format "1d2h3m4s" to milliseconds.
     *
     * @param timeString The time string to parse
     * @return The time in milliseconds, or -1 if the string is invalid
     */
    public static long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }
        
        // Handle "permanent" or "forever" cases
        if (timeString.equalsIgnoreCase("permanent") || 
            timeString.equalsIgnoreCase("forever") || 
            timeString.equalsIgnoreCase("perm")) {
            return Long.MAX_VALUE;
        }
        
        // Try to parse as a number (seconds)
        try {
            return TimeUnit.SECONDS.toMillis(Long.parseLong(timeString));
        } catch (NumberFormatException ignored) {
            // Not a simple number, continue with pattern matching
        }
        
        // Parse using the pattern
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());
        if (matcher.matches()) {
            long totalMillis = 0;
            
            String days = matcher.group(1);
            if (days != null && !days.isEmpty()) {
                totalMillis += TimeUnit.DAYS.toMillis(Long.parseLong(days));
            }
            
            String hours = matcher.group(2);
            if (hours != null && !hours.isEmpty()) {
                totalMillis += TimeUnit.HOURS.toMillis(Long.parseLong(hours));
            }
            
            String minutes = matcher.group(3);
            if (minutes != null && !minutes.isEmpty()) {
                totalMillis += TimeUnit.MINUTES.toMillis(Long.parseLong(minutes));
            }
            
            String seconds = matcher.group(4);
            if (seconds != null && !seconds.isEmpty()) {
                totalMillis += TimeUnit.SECONDS.toMillis(Long.parseLong(seconds));
            }
            
            return totalMillis > 0 ? totalMillis : -1;
        }
        
        return -1; // Invalid format
    }
    
    /**
     * Formats a duration in milliseconds to a human-readable string.
     *
     * @param durationMillis The duration in milliseconds
     * @return The formatted duration string
     */
    public static String formatDuration(long durationMillis) {
        if (durationMillis <= 0) {
            return "0 seconds";
        }
        
        if (durationMillis == Long.MAX_VALUE) {
            return "permanent";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        durationMillis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        durationMillis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        durationMillis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
            if (hours > 0 || minutes > 0 || seconds > 0) {
                sb.append(", ");
            }
        }
        
        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
            if (minutes > 0 || seconds > 0) {
                sb.append(", ");
            }
        }
        
        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            if (seconds > 0) {
                sb.append(", ");
            }
        }
        
        if (seconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a duration in milliseconds to a compact string (e.g., "1d 2h 3m 4s").
     *
     * @param durationMillis The duration in milliseconds
     * @return The formatted duration string
     */
    public static String formatDurationCompact(long durationMillis) {
        if (durationMillis <= 0) {
            return "0s";
        }
        
        if (durationMillis == Long.MAX_VALUE) {
            return "permanent";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        durationMillis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        durationMillis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        durationMillis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        
        sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }
    
    /**
     * Formats a timestamp to a relative time string (e.g., "2 hours ago").
     *
     * @param timestamp The timestamp in milliseconds
     * @return The formatted relative time string
     */
    public static String formatRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 0) {
            return "in the future";
        }
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "just now";
        }
        
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        
        if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        
        if (diff < TimeUnit.DAYS.toMillis(30)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        }
        
        if (diff < TimeUnit.DAYS.toMillis(365)) {
            long months = TimeUnit.MILLISECONDS.toDays(diff) / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        }
        
        long years = TimeUnit.MILLISECONDS.toDays(diff) / 365;
        return years + (years == 1 ? " year ago" : " years ago");
    }
    
    /**
     * Formats time remaining from a future timestamp.
     *
     * @param futureTimestamp The future timestamp in milliseconds
     * @return The formatted time remaining string, or "Expired" if in the past
     */
    public static String formatTimeRemaining(long futureTimestamp) {
        long now = System.currentTimeMillis();
        long diff = futureTimestamp - now;
        
        if (diff <= 0) {
            return "Expired";
        }
        
        return formatDuration(diff);
    }
} 