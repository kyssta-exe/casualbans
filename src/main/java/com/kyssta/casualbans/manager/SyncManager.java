package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.storage.SqlStorage;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cross-server synchronization via shared SQL database.
 * Uses a cb_sync table to broadcast punishment create/remove events
 * that other server instances pick up on a polling interval.
 */
public class SyncManager {

    private final CasualBans plugin;
    private final boolean enabled;
    private final String serverName;

    private static final long POLL_MS = 2000;
    private static final String SYNC_TABLE = "cb_sync";

    public SyncManager(CasualBans plugin) {
        this.plugin = plugin;
        this.serverName = plugin.getServerName();
        this.enabled = plugin.getConfigManager().isSyncEnabled()
            && plugin.getStorageProvider() instanceof SqlStorage;
        if (enabled) {
            initTable();
            startPoller();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private SqlStorage sql() {
        return (SqlStorage) plugin.getStorageProvider();
    }

    private DataSource ds() {
        return sql().getDataSource();
    }

    private void initTable() {
        try (Connection c = ds().getConnection();
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS " + SYNC_TABLE + " ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "action VARCHAR(10) NOT NULL, "
                + "punishment_id BIGINT NOT NULL, "
                + "origin_server VARCHAR(64) NOT NULL, "
                + "created_at BIGINT NOT NULL"
                + ")");
        } catch (SQLException e) {
            plugin.getLogger().warning("Sync table init failed: " + e.getMessage());
        }
    }

    private void startPoller() {
        plugin.getAsyncExecutor().scheduleAtFixedRate(this::pollSync, POLL_MS, POLL_MS, TimeUnit.MILLISECONDS);
        plugin.getLogger().info("Cross-server sync enabled (polling every 2s).");
    }

    /**
     * Write a sync entry so other servers know about a punishment change.
     */
    public void notifyPunishmentCreated(Punishment punishment) {
        if (!enabled) return;
        writeSyncEntry("CREATE", punishment.getId());
    }

    public void notifyPunishmentRemoved(Punishment punishment) {
        if (!enabled) return;
        writeSyncEntry("REMOVE", punishment.getId());
    }

    private void writeSyncEntry(String action, long punishmentId) {
        try (Connection c = ds().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO " + SYNC_TABLE + " (action, punishment_id, origin_server, created_at) VALUES (?,?,?,?)")) {
            ps.setString(1, action);
            ps.setLong(2, punishmentId);
            ps.setString(3, serverName);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Sync write failed: " + e.getMessage());
        }
    }

    /**
     * Poll for sync entries from OTHER servers and process them.
     * Skips entries from this server (origin_server = serverName).
     */
    private void pollSync() {
        try (Connection c = ds().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id, action, punishment_id, origin_server FROM " + SYNC_TABLE
                     + " WHERE origin_server != ? ORDER BY id ASC LIMIT 50")) {
            ps.setString(1, serverName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long syncId = rs.getLong("id");
                String action = rs.getString("action");
                long punId = rs.getLong("punishment_id");
                processSync(action, punId);
                deleteSyncEntry(syncId);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Sync poll failed: " + e.getMessage());
        }
    }

    private void processSync(String action, long punishmentId) {
        if (action.equals("REMOVE")) {
            Punishment p = plugin.getStorageProvider().getPunishment(punishmentId);
            if (p != null) {
                p.setActive(false);
                plugin.getStorageProvider().savePunishment(p);
            }
        }
        // For CREATE, the punishment is already in the shared DB — 
        // other servers just need to refresh their caches
    }

    private void deleteSyncEntry(long id) {
        try (Connection c = ds().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + SYNC_TABLE + " WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // ignore — cleanup failures are non-critical
        }
    }

    /**
     * Cleanup old sync entries (older than 1 hour).
     */
    public void cleanup() {
        if (!enabled) return;
        long cutoff = System.currentTimeMillis() - 3600000;
        try (Connection c = ds().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM " + SYNC_TABLE + " WHERE created_at < ?")) {
            ps.setLong(1, cutoff);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Sync cleanup failed: " + e.getMessage());
        }
    }
}
