package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Manages MaxMind GeoLite2 GeoIP lookups for determining player country
 * and enforcing country-based connection rules.
 */
public class GeoIPManager {

    private final CasualBans plugin;
    private DatabaseReader databaseReader;
    private boolean available;

    public GeoIPManager(CasualBans plugin) {
        this.plugin = plugin;
        this.available = false;
        loadDatabase();
    }

    /**
     * Attempt to load the MaxMind GeoLite2 database from the plugin data folder.
     * Looks for GeoLite2-Country.mmdb (or .mmdb.gz) in the plugin directory.
     */
    private void loadDatabase() {
        if (!plugin.getConfigManager().isGeoipEnabled()) {
            plugin.getLogger().info("GeoIP is disabled in config.");
            return;
        }

        File databaseFile = findDatabaseFile();
        if (databaseFile == null) {
            plugin.getLogger().warning("GeoIP database file not found. Place GeoLite2-Country.mmdb in the plugin folder.");
            this.available = false;
            return;
        }

        try {
            this.databaseReader = new DatabaseReader.Builder(databaseFile)
                .build();
            this.available = true;
            plugin.getLogger().info("GeoIP database loaded: " + databaseFile.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load GeoIP database: " + e.getMessage());
            this.available = false;
        }
    }

    /**
     * Look for GeoLite2-Country.mmdb in the plugin data folder.
     */
    private File findDatabaseFile() {
        // Check for plain .mmdb
        File plain = new File(plugin.getDataFolder(), "GeoLite2-Country.mmdb");
        if (plain.exists() && plain.isFile()) {
            return plain;
        }

        // Also check the data subdirectory
        File dataDir = new File(plugin.getDataFolder(), "data");
        File inData = new File(dataDir, "GeoLite2-Country.mmdb");
        if (inData.exists() && inData.isFile()) {
            return inData;
        }

        return null;
    }

    /**
     * Look up the country name for a given IP address.
     *
     * @param ip the IP address to look up
     * @return the country name (e.g., "United States"), or "Unknown" if
     *         the database is unavailable or the IP could not be resolved
     */
    public String getCountry(String ip) {
        if (!available || databaseReader == null) {
            return "Unknown";
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            CountryResponse response = databaseReader.country(address);
            if (response == null || response.getCountry() == null) {
                return "Unknown";
            }
            String country = response.getCountry().getName();
            return country != null ? country : "Unknown";
        } catch (IOException | GeoIp2Exception e) {
            return "Unknown";
        }
    }

    /**
     * Check whether connections from the given IP should be blocked based on
     * the configured GeoIP blacklist / whitelist.
     *
     * @param ip the IP address to check
     * @return true if the IP's country is blocked by policy
     */
    public boolean isBlocked(String ip) {
        if (!available) {
            return false;
        }

        ConfigManager config = plugin.getConfigManager();
        String country = getCountry(ip);

        if (country == null || country.equals("Unknown")) {
            // If whitelist mode contains "error", unresolvable IPs are allowed.
            return !config.getGeoipWhitelist().contains("error")
                && !config.getGeoipWhitelist().isEmpty();
        }

        // Blacklist mode: block if country is in the blacklist
        if (!config.getGeoipBlacklist().isEmpty()) {
            return config.getGeoipBlacklist().stream()
                .anyMatch(b -> b.equalsIgnoreCase(country));
        }

        // Whitelist mode: block if country is NOT in the whitelist
        if (!config.getGeoipWhitelist().isEmpty()) {
            return config.getGeoipWhitelist().stream()
                .noneMatch(w -> w.equalsIgnoreCase(country));
        }

        return false;
    }

    /**
     * Check whether the GeoIP database is loaded and usable.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Reload the GeoIP database (e.g., on plugin reload).
     */
    public void reload() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
            } catch (IOException ignored) {
            }
        }
        databaseReader = null;
        available = false;
        loadDatabase();
    }
}
