package com.kyssta.casualbans.storage;

import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.model.StaffNote;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Abstraction for punishment data storage.
 * Supports JSON (default) and SQL (MySQL, MariaDB, PostgreSQL, H2).
 */
public interface StorageProvider {

    void initialize();

    void shutdown();

    boolean isConnected();

    // ── Punishment CRUD ──

    void savePunishment(Punishment punishment);

    void removePunishment(long id);

    void updatePunishment(Punishment punishment);

    Punishment getPunishment(long id);

    // ── Active Punishment Queries ──

    List<Punishment> getActivePunishments(UUID uuid, PunishmentType type, String serverScope);

    List<Punishment> getActivePunishmentsByIP(String ip, PunishmentType type, String serverScope);

    List<Punishment> getAllActivePunishments(PunishmentType type, String serverScope);

    boolean isPlayerBanned(UUID uuid, String serverScope);

    boolean isPlayerMuted(UUID uuid, String serverScope);

    boolean isPlayerWarned(UUID uuid, String serverScope);

    // ── History Queries ──

    List<Punishment> getHistory(UUID uuid, int limit);

    List<Punishment> getHistoryByType(UUID uuid, PunishmentType type, int limit);

    List<Punishment> getStaffHistory(UUID executorUUID, int limit);

    List<Punishment> getStaffHistoryByType(UUID executorUUID, PunishmentType type, int limit);

    int getOffenseCount(UUID uuid, String templateName, String serverScope);

    int getOffenseCountByIP(String ip, String templateName);

    // ── Alt Detection / IP Tracking ──

    List<UUID> getAccountsByIP(String ip);

    List<String> getIPsByUUID(UUID uuid);

    void recordIPAddress(UUID uuid, String ip, String playerName);

    List<IPRecord> getIPHistory(UUID uuid);

    // ── Name History ──

    void recordNameChange(UUID uuid, String oldName, String newName);

    List<NameRecord> getNameHistory(UUID uuid);

    // ── Punishment Counts ──

    int getTotalPunishments(PunishmentType type);

    int getActivePunishmentCount(PunishmentType type);

    /**
     * Returns every punishment record in storage, regardless of active
     * status or expiry.  Used for full exports and migration.
     */
    List<Punishment> getAllPunishments();

    // ── Staff Rollback ──

    int rollbackStaff(UUID executorUUID, long olderThan);

    int pruneHistory(UUID uuid, long olderThan);

    // ── IP Ban Count (for alt bans) ──

    boolean isIPBanned(String ip, String serverScope);

    // ── Staff Notes ──

    void saveNote(StaffNote note);

    void deleteNote(long id);

    List<StaffNote> getNotes(UUID targetUUID);

    List<StaffNote> getAllNotes();

    // ── Data Classes ──

    record IPRecord(UUID uuid, String playerName, String ip, long timestamp) {}
    record NameRecord(String oldName, String newName, long timestamp) {}
}
