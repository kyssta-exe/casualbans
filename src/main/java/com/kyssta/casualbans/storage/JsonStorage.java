package com.kyssta.casualbans.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.model.StaffNote;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * JSON-file-backed implementation of {@link StorageProvider}.
 * <p>
 * Stores all data under {@code plugins/CasualBans/data/}:
 * <ul>
 *   <li>{@code punishments.json} — all punishment records</li>
 *   <li>{@code ip_history.json} — IP/alt-detection records</li>
 *   <li>{@code name_history.json} — player name-change records</li>
 * </ul>
 * Data is held in memory and written asynchronously via
 * {@link CasualBans#getIoExecutor()} on every mutation. A full
 * synchronous flush occurs on {@link #shutdown()}.
 */
public class JsonStorage implements StorageProvider {

    private final CasualBans plugin;
    private final Gson gson;
    private final Path dataDir;
    private final Path punishmentsPath;
    private final Path ipHistoryPath;
    private final Path nameHistoryPath;
    private final Path notesPath;

    // ── In-memory stores ──

    /** All punishments keyed by their unique ID. */
    private final ConcurrentHashMap<Long, Punishment> punishments = new ConcurrentHashMap<>();

    /** IP-association records. */
    private final CopyOnWriteArrayList<IPRecord> ipHistory = new CopyOnWriteArrayList<>();

    /**
     * Name-change records.  Stores the player UUID alongside the change
     * because the interface's {@link NameRecord} does not carry one.
     */
    private final CopyOnWriteArrayList<StoredNameRecord> storedNameHistory = new CopyOnWriteArrayList<>();

    /** Staff notes keyed by their unique ID. */
    private final ConcurrentHashMap<Long, StaffNote> notes = new ConcurrentHashMap<>();

    /** Auto-incrementing counter for punishment IDs. */
    private final AtomicLong nextId = new AtomicLong(1);

    /** Auto-incrementing counter for note IDs. */
    private final AtomicLong nextNoteId = new AtomicLong(1);

    // ── Constructor ──────────────────────────────────────────────────────────

    public JsonStorage(CasualBans plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataDir = plugin.getDataFolder().toPath().resolve("data");
        this.punishmentsPath = dataDir.resolve("punishments.json");
        this.ipHistoryPath = dataDir.resolve("ip_history.json");
        this.nameHistoryPath = dataDir.resolve("name_history.json");
        this.notesPath = dataDir.resolve("notes.json");
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void initialize() {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not create data directory: " + e.getMessage());
        }
        loadAll();
        plugin.getLogger().info("JSON storage loaded — " + punishments.size()
                + " punishments, " + ipHistory.size() + " IP records, "
                + storedNameHistory.size() + " name records, "
                + notes.size() + " staff notes.");
    }

    @Override
    public void shutdown() {
        saveAllSync();
        plugin.getLogger().info("JSON storage saved.");
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    // ── Punishment CRUD ──────────────────────────────────────────────────────

    @Override
    public synchronized void savePunishment(Punishment punishment) {
        Objects.requireNonNull(punishment, "punishment");
        if (punishment.getId() <= 0) {
            punishment.setId(nextId.getAndIncrement());
        } else {
            long id = punishment.getId();
            if (id >= nextId.get()) {
                nextId.set(id + 1);
            }
        }
        punishments.put(punishment.getId(), punishment);
        savePunishmentsAsync();
    }

    @Override
    public synchronized void removePunishment(long id) {
        punishments.remove(id);
        savePunishmentsAsync();
    }

    @Override
    public synchronized void updatePunishment(Punishment punishment) {
        Objects.requireNonNull(punishment, "punishment");
        punishments.put(punishment.getId(), punishment);
        savePunishmentsAsync();
    }

    @Override
    public Punishment getPunishment(long id) {
        return punishments.get(id);
    }

    // ── Active Punishment Queries ────────────────────────────────────────────

    @Override
    public List<Punishment> getActivePunishments(UUID uuid, PunishmentType type, String serverScope) {
        return punishments.values().stream()
                .filter(p -> p.getUuid() != null && p.getUuid().equals(uuid))
                .filter(p -> type == null || p.getType() == type)
                .filter(Punishment::isActive)
                .filter(p -> !p.isExpired())
                .filter(p -> scopeMatches(p.getServerScope(), serverScope))
                .collect(Collectors.toList());
    }

    @Override
    public List<Punishment> getActivePunishmentsByIP(String ip, PunishmentType type, String serverScope) {
        if (ip == null) return List.of();
        return punishments.values().stream()
                .filter(p -> ip.equals(p.getIp()))
                .filter(p -> type == null || p.getType() == type)
                .filter(Punishment::isActive)
                .filter(p -> !p.isExpired())
                .filter(p -> scopeMatches(p.getServerScope(), serverScope))
                .collect(Collectors.toList());
    }

    @Override
    public List<Punishment> getAllActivePunishments(PunishmentType type, String serverScope) {
        return punishments.values().stream()
                .filter(p -> type == null || p.getType() == type)
                .filter(Punishment::isActive)
                .filter(p -> !p.isExpired())
                .filter(p -> scopeMatches(p.getServerScope(), serverScope))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPlayerBanned(UUID uuid, String serverScope) {
        return punishments.values().stream().anyMatch(p ->
                p.getUuid() != null && p.getUuid().equals(uuid)
                        && (p.getType() == PunishmentType.BAN
                        || p.getType() == PunishmentType.TEMPBAN
                        || p.getType() == PunishmentType.IPBAN)
                        && p.isActive() && !p.isExpired()
                        && scopeMatches(p.getServerScope(), serverScope));
    }

    @Override
    public boolean isPlayerMuted(UUID uuid, String serverScope) {
        return punishments.values().stream().anyMatch(p ->
                p.getUuid() != null && p.getUuid().equals(uuid)
                        && (p.getType() == PunishmentType.MUTE
                        || p.getType() == PunishmentType.TEMPMUTE
                        || p.getType() == PunishmentType.IPMUTE)
                        && p.isActive() && !p.isExpired()
                        && scopeMatches(p.getServerScope(), serverScope));
    }

    @Override
    public boolean isPlayerWarned(UUID uuid, String serverScope) {
        return punishments.values().stream().anyMatch(p ->
                p.getUuid() != null && p.getUuid().equals(uuid)
                        && p.getType() == PunishmentType.WARN
                        && p.isActive() && !p.isExpired()
                        && scopeMatches(p.getServerScope(), serverScope));
    }

    // ── History Queries ──────────────────────────────────────────────────────

    @Override
    public List<Punishment> getHistory(UUID uuid, int limit) {
        return punishments.values().stream()
                .filter(p -> p.getUuid() != null && p.getUuid().equals(uuid))
                .sorted(Comparator.comparingLong(Punishment::getDateStart).reversed())
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Punishment> getHistoryByType(UUID uuid, PunishmentType type, int limit) {
        return punishments.values().stream()
                .filter(p -> p.getUuid() != null && p.getUuid().equals(uuid))
                .filter(p -> p.getType() == type)
                .sorted(Comparator.comparingLong(Punishment::getDateStart).reversed())
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Punishment> getStaffHistory(UUID executorUUID, int limit) {
        return punishments.values().stream()
                .filter(p -> p.getExecutorUUID() != null && p.getExecutorUUID().equals(executorUUID))
                .sorted(Comparator.comparingLong(Punishment::getDateStart).reversed())
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public List<Punishment> getStaffHistoryByType(UUID executorUUID, PunishmentType type, int limit) {
        return punishments.values().stream()
                .filter(p -> p.getExecutorUUID() != null && p.getExecutorUUID().equals(executorUUID))
                .filter(p -> p.getType() == type)
                .sorted(Comparator.comparingLong(Punishment::getDateStart).reversed())
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    @Override
    public int getOffenseCount(UUID uuid, String templateName, String serverScope) {
        return (int) punishments.values().stream()
                .filter(p -> p.getUuid() != null && p.getUuid().equals(uuid))
                .filter(p -> templateName.equals(p.getTemplateName()))
                .filter(Punishment::isActive)
                .filter(p -> !p.isExpired())
                .filter(p -> scopeMatches(p.getServerScope(), serverScope))
                .count();
    }

    @Override
    public int getOffenseCountByIP(String ip, String templateName) {
        return (int) punishments.values().stream()
                .filter(p -> ip.equals(p.getIp()))
                .filter(p -> templateName.equals(p.getTemplateName()))
                .filter(Punishment::isActive)
                .filter(p -> !p.isExpired())
                .count();
    }

    // ── Alt Detection / IP Tracking ──────────────────────────────────────────

    @Override
    public List<UUID> getAccountsByIP(String ip) {
        return ipHistory.stream()
                .filter(r -> r.ip().equals(ip))
                .map(IPRecord::uuid)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getIPsByUUID(UUID uuid) {
        return ipHistory.stream()
                .filter(r -> r.uuid().equals(uuid))
                .map(IPRecord::ip)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void recordIPAddress(UUID uuid, String ip, String playerName) {
        ipHistory.add(new IPRecord(uuid, playerName, ip, System.currentTimeMillis()));
        saveIPHistoryAsync();
    }

    @Override
    public List<IPRecord> getIPHistory(UUID uuid) {
        return ipHistory.stream()
                .filter(r -> r.uuid().equals(uuid))
                .sorted(Comparator.comparingLong(IPRecord::timestamp).reversed())
                .collect(Collectors.toList());
    }

    // ── Name History ─────────────────────────────────────────────────────────

    @Override
    public void recordNameChange(UUID uuid, String oldName, String newName) {
        storedNameHistory.add(new StoredNameRecord(uuid, oldName, newName, System.currentTimeMillis()));
        saveNameHistoryAsync();
    }

    @Override
    public List<NameRecord> getNameHistory(UUID uuid) {
        return storedNameHistory.stream()
                .filter(r -> r.uuid.equals(uuid))
                .sorted(Comparator.comparingLong((StoredNameRecord r) -> r.timestamp).reversed())
                .map(r -> new NameRecord(r.oldName, r.newName, r.timestamp))
                .collect(Collectors.toList());
    }

    // ── Punishment Counts ────────────────────────────────────────────────────

    @Override
    public int getTotalPunishments(PunishmentType type) {
        return (int) punishments.values().stream()
                .filter(p -> type == null || p.getType() == type)
                .count();
    }

    @Override
    public int getActivePunishmentCount(PunishmentType type) {
        return (int) punishments.values().stream()
                .filter(p -> type == null || p.getType() == type)
                .filter(Punishment::isActive)
                .filter(p -> !p.isExpired())
                .count();
    }

    @Override
    public List<Punishment> getAllPunishments() {
        return punishments.values().stream()
                .sorted(Comparator.comparingLong(Punishment::getId))
                .collect(Collectors.toList());
    }

    // ── Staff Rollback ───────────────────────────────────────────────────────

    @Override
    public synchronized int rollbackStaff(UUID executorUUID, long olderThan) {
        int count = 0;
        for (Punishment p : punishments.values()) {
            if (p.getExecutorUUID() != null && p.getExecutorUUID().equals(executorUUID)
                    && p.getDateStart() < olderThan && p.isActive()) {
                p.setActive(false);
                count++;
            }
        }
        if (count > 0) {
            savePunishmentsAsync();
        }
        return count;
    }

    @Override
    public synchronized int pruneHistory(UUID uuid, long olderThan) {
        List<Long> toRemove = new ArrayList<>();
        for (Punishment p : punishments.values()) {
            if (p.getUuid() != null && p.getUuid().equals(uuid)
                    && p.getDateStart() < olderThan) {
                toRemove.add(p.getId());
            }
        }
        toRemove.forEach(punishments::remove);
        if (!toRemove.isEmpty()) {
            savePunishmentsAsync();
        }
        return toRemove.size();
    }

    // ── IP Ban Check ─────────────────────────────────────────────────────────

    @Override
    public boolean isIPBanned(String ip, String serverScope) {
        return punishments.values().stream().anyMatch(p ->
                ip.equals(p.getIp())
                        && p.getType() == PunishmentType.IPBAN
                        && p.isActive() && !p.isExpired()
                        && scopeMatches(p.getServerScope(), serverScope));
    }

    // ── Staff Notes ──────────────────────────────────────────────────────────

    @Override
    public synchronized void saveNote(StaffNote note) {
        Objects.requireNonNull(note, "note");
        if (note.getId() <= 0) {
            note.setId(nextNoteId.getAndIncrement());
        } else {
            long id = note.getId();
            if (id >= nextNoteId.get()) {
                nextNoteId.set(id + 1);
            }
        }
        notes.put(note.getId(), note);
        saveNotesAsync();
    }

    @Override
    public synchronized void deleteNote(long id) {
        notes.remove(id);
        saveNotesAsync();
    }

    @Override
    public List<StaffNote> getNotes(UUID targetUUID) {
        return notes.values().stream()
                .filter(n -> n.getTargetUUID() != null && n.getTargetUUID().equals(targetUUID))
                .sorted(Comparator.comparingLong(StaffNote::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffNote> getAllNotes() {
        return notes.values().stream()
                .sorted(Comparator.comparingLong(StaffNote::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns true when the punishment's scope matches the query scope. */
    private static boolean scopeMatches(String punishmentScope, String queryScope) {
        if (queryScope == null || queryScope.isEmpty() || queryScope.equals("*")) return true;
        if (punishmentScope == null || punishmentScope.isEmpty() || punishmentScope.equals("*")) return true;
        return punishmentScope.equals(queryScope);
    }

    // ── File I/O ─────────────────────────────────────────────────────────────

    private void loadAll() {
        loadPunishments();
        loadIPHistory();
        loadNameHistory();
        loadNotes();
    }

    private void loadPunishments() {
        if (!Files.exists(punishmentsPath)) return;
        try (Reader reader = Files.newBufferedReader(punishmentsPath, StandardCharsets.UTF_8)) {
            PunishmentStore store = gson.fromJson(reader, PunishmentStore.class);
            if (store != null) {
                nextId.set(Math.max(nextId.get(), store.nextId));
                if (store.punishments != null) {
                    for (Punishment p : store.punishments) {
                        if (p.getId() > 0) {
                            punishments.put(p.getId(), p);
                            if (p.getId() >= nextId.get()) {
                                nextId.set(p.getId() + 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load " + punishmentsPath + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadIPHistory() {
        if (!Files.exists(ipHistoryPath)) return;
        try (Reader reader = Files.newBufferedReader(ipHistoryPath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<IPRecord>>() {}.getType();
            List<IPRecord> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                ipHistory.addAll(loaded);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load " + ipHistoryPath + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadNameHistory() {
        if (!Files.exists(nameHistoryPath)) return;
        try (Reader reader = Files.newBufferedReader(nameHistoryPath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<StoredNameRecord>>() {}.getType();
            List<StoredNameRecord> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                storedNameHistory.addAll(loaded);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load " + nameHistoryPath + ": " + e.getMessage());
        }
    }

    private void saveAllSync() {
        savePunishmentsSync();
        saveIPHistorySync();
        saveNameHistorySync();
        saveNotesSync();
    }

    private synchronized void savePunishmentsSync() {
        try {
            PunishmentStore store = new PunishmentStore();
            store.nextId = nextId.get();
            store.punishments = new ArrayList<>(punishments.values());
            Files.writeString(punishmentsPath, gson.toJson(store), StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save punishments: " + e.getMessage());
        }
    }

    private synchronized void saveIPHistorySync() {
        try {
            Files.writeString(ipHistoryPath, gson.toJson(new ArrayList<>(ipHistory)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save IP history: " + e.getMessage());
        }
    }

    private synchronized void saveNameHistorySync() {
        try {
            Files.writeString(nameHistoryPath, gson.toJson(new ArrayList<>(storedNameHistory)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save name history: " + e.getMessage());
        }
    }

    private void savePunishmentsAsync() {
        plugin.getIoExecutor().execute(this::savePunishmentsSync);
    }

    private void saveIPHistoryAsync() {
        plugin.getIoExecutor().execute(this::saveIPHistorySync);
    }

    private void saveNameHistoryAsync() {
        plugin.getIoExecutor().execute(this::saveNameHistorySync);
    }

    private void loadNotes() {
        if (!Files.exists(notesPath)) return;
        try (Reader reader = Files.newBufferedReader(notesPath, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<StaffNote>>() {}.getType();
            List<StaffNote> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                for (StaffNote note : loaded) {
                    if (note.getId() > 0) {
                        notes.put(note.getId(), note);
                        if (note.getId() >= nextNoteId.get()) {
                            nextNoteId.set(note.getId() + 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load " + notesPath + ": " + e.getMessage());
        }
    }

    private synchronized void saveNotesSync() {
        try {
            Files.writeString(notesPath, gson.toJson(new ArrayList<>(notes.values())), StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save notes: " + e.getMessage());
        }
    }

    private void saveNotesAsync() {
        plugin.getIoExecutor().execute(this::saveNotesSync);
    }

    // ── JSON Wrappers ────────────────────────────────────────────────────────

    /**
     * Container for the punishments file, holding the auto-increment counter
     * alongside the list of {@link Punishment} objects.
     */
    private static class PunishmentStore {
        long nextId;
        List<Punishment> punishments;
    }

    /**
     * Internal name-change record that includes the player {@link UUID},
     * because the interface-level {@link NameRecord} does not carry one.
     * Serialised to {@code name_history.json} and converted to
     * {@link NameRecord} on read.
     */
    private static class StoredNameRecord {
        UUID uuid;
        String oldName;
        String newName;
        long timestamp;

        StoredNameRecord() {}

        StoredNameRecord(UUID uuid, String oldName, String newName, long timestamp) {
            this.uuid = uuid;
            this.oldName = oldName;
            this.newName = newName;
            this.timestamp = timestamp;
        }
    }
}
