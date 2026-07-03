package com.kyssta.casualbans.util;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.storage.SqlStorage;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Configuration and input validation utilities for CasualBans.
 * <p>
 * Provides methods for validating configuration values, player names,
 * and duration strings with human-readable error messages.
 */
public final class Validator {

    private static final Set<String> VALID_DRIVERS = new HashSet<>(Arrays.asList(
        "JSON", "MYSQL", "MARIADB", "POSTGRESQL", "H2"
    ));

    private static final java.util.regex.Pattern INVALID_NAME_CHARS =
        java.util.regex.Pattern.compile("[^a-zA-Z0-9_]");

    private Validator() {
    }

    /**
     * Validate the plugin configuration on startup.
     * Checks storage driver validity and, for SQL drivers, verifies connectivity.
     *
     * @return true if all checks pass, false if any issues were logged
     */
    public static boolean validateConfig() {
        CasualBans plugin = CasualBans.getInstance();
        FileConfiguration config = plugin.getConfig();
        boolean valid = true;

        // Validate storage driver
        String driver = config.getString("storage.driver", "JSON");
        if (!VALID_DRIVERS.contains(driver.toUpperCase())) {
            plugin.getLogger().severe("Invalid storage.driver: '" + driver
                + "'. Must be one of: JSON, MYSQL, MARIADB, POSTGRESQL, H2. "
                + "Falling back to JSON.");
            config.set("storage.driver", "JSON");
            valid = false;
        }

        // Validate SQL connection if using an SQL driver
        if (isSqlDriver(driver)) {
            valid &= validateSqlConnection(plugin);
        }

        // Validate sync configuration consistency
        if (config.getBoolean("sync.enabled", true) && !isSqlDriver(driver)) {
            plugin.getLogger().warning("Sync is enabled but storage driver is '"
                + driver + "'. Sync requires an SQL database (MYSQL, MARIADB, POSTGRESQL, H2).");
            valid = false;
        }

        return valid;
    }

    /**
     * Validate a player name for null, empty, or invalid characters.
     *
     * @param name the player name to validate
     * @return an error message if invalid, or null if the name is valid
     */
    public static String validatePlayerName(String name) {
        if (name == null) {
            return "Player name cannot be null.";
        }
        if (name.isEmpty()) {
            return "Player name cannot be empty.";
        }
        if (name.length() > 16) {
            return "Player name is too long (max 16 characters).";
        }
        if (INVALID_NAME_CHARS.matcher(name).find()) {
            return "Player name contains invalid characters. Only letters, numbers, and underscores are allowed.";
        }
        return null;
    }

    /**
     * Validate a duration string and return a human-readable error message.
     *
     * @param input the duration string (e.g., "7d", "30m", "permanent")
     * @return null if valid, or an error message describing the issue
     */
    public static String validateDuration(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Duration cannot be empty.";
        }

        String trimmed = input.trim();

        // "permanent" is always valid
        if (trimmed.equalsIgnoreCase("permanent")) {
            return null;
        }

        // Check for non-digit, non-letter characters
        if (!trimmed.matches("[\\d]+\\s*[a-zA-Z]+")) {
            return "Invalid duration format. Examples: 'permanent', '7d', '30m', '2h', '14d', '3w', '6mo', '1y'.";
        }

        // Parse and verify the duration yields a positive value
        long millis = TimeUtil.parseDuration(trimmed);
        if (millis <= 0) {
            return "Invalid duration: '" + trimmed
                + "'. Could not parse any time units. Use 's' (seconds), 'm' (minutes), 'h' (hours), "
                + "'d' (days), 'w' (weeks), 'mo' (months), 'y' (years), or 'permanent'.";
        }

        return null;
    }

    /**
     * Check whether a driver string refers to an SQL-based storage backend.
     */
    private static boolean isSqlDriver(String driver) {
        return switch (driver.toUpperCase()) {
            case "MYSQL", "MARIADB", "POSTGRESQL", "H2" -> true;
            default -> false;
        };
    }

    /**
     * Attempt a lightweight SQL connection test and log the result.
     */
    private static boolean validateSqlConnection(CasualBans plugin) {
        if (!(plugin.getStorageProvider() instanceof SqlStorage sqlStorage)) {
            plugin.getLogger().severe("Storage driver is set to an SQL type but storage provider is not SqlStorage.");
            return false;
        }

        try (Connection conn = sqlStorage.getDataSource().getConnection()) {
            if (conn.isValid(5)) {
                plugin.getLogger().info("SQL connection validated successfully.");
                return true;
            } else {
                plugin.getLogger().severe("SQL connection is not valid. Check your storage.sql settings in config.yml.");
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                "SQL connection test failed: " + e.getMessage()
                    + ". Check your storage.sql settings in config.yml.", e);
            return false;
        }
    }
}
