# Developer API

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans provides a **first-class developer API** for integrating punishment data into your own plugins. The API lets you query punishments, listen for events, and manage punishment records programmatically.

---

## Maven Dependency

Add the CasualBans API to your project:

```xml
<repositories>
  <repository>
    <id>kyssta-repo</id>
    <url>https://repo.kyssta.dev/releases</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>dev.kyssta</groupId>
    <artifactId>casualbans-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

## Gradle Dependency

```kotlin
repositories {
    maven("https://repo.kyssta.dev/releases")
}

dependencies {
    compileOnly("dev.kyssta:casualbans-api:1.0.0")
}
```

---

## Getting the API Instance

Access the API from any plugin:

```java
import dev.kyssta.casualbans.api.CasualBansAPI;

CasualBansAPI api = CasualBansAPI.getInstance();
```

## StorageProvider — Querying Punishments

The `StorageProvider` interface provides all query methods:

```java
import dev.kyssta.casualbans.api.Database;

Database db = api.getStorage();

// Check if a player is banned
boolean banned = db.isPlayerBanned(UUID.fromString("..."));
boolean bannedByName = db.isPlayerBanned("Notch");

// Check if a player is muted
boolean muted = db.isPlayerMuted(uuid);

// Get all IP addresses a player has used
List<String> ips = db.getUsersByIP(uuid);
List<String> alts = db.getUsersByIP("1.2.3.4");  // Also works by IP string

// Get active punishments for a player
List<Entry> activeBans = db.getActiveBans(uuid);
List<Entry> activeMutes = db.getActiveMutes(uuid);
List<Entry> warnings = db.getWarnings(uuid);

// Get full history
List<Entry> history = db.getHistory(uuid, page, pageSize);
```

## Entry — Punishment Data Model

```java
import dev.kyssta.casualbans.api.Entry;

public class Entry {
    UUID getId();
    UUID getPlayerUUID();
    String getPlayerName();
    UUID getStaffUUID();
    String getStaffName();
    String getReason();
    PunishmentType getType();     // BAN, TEMPBAN, IPBAN, MUTE, TEMPMUTE, IPMUTE, WARN, KICK
    long getDate();               // Unix timestamp (ms)
    Long getExpires();            // Unix timestamp (ms) — null if permanent
    boolean isPermanent();
    boolean isActive();
    String getServer();
    String getServerGroup();
    List<String> getFlags();      // silent, extra_silent, public, etc.
}
```

## PunishmentManager — Issuing Punishments

```java
import dev.kyssta.casualbans.api.PunishmentManager;

PunishmentManager pm = api.getPunishmentManager();

// Ban a player permanently
pm.ban(targetUUID, staffUUID, "Griefing", false);

// Temp-ban for 7 days
pm.tempBan(targetUUID, staffUUID, "Repeated griefing", "7d", true);  // silent=true

// Mute
pm.mute(targetUUID, staffUUID, "Chat spam", false);

// Warn
pm.warn(targetUUID, staffUUID, "First warning", false);

// Unpunish
pm.unban(targetUUID, staffUUID);
pm.unmute(targetUUID, staffUUID);
pm.unwarn(targetUUID, warningId, staffUUID);
```

---

## Events — Listening for Punishments

Register event listeners through the `Events` API:

```java
import dev.kyssta.casualbans.api.Events;

Events events = api.getEvents();

// Listen for new punishments
events.registerListener(new Events.Listener() {
    @Override
    public void entryAdded(Entry entry) {
        // Called when any punishment is issued
        getLogger().info(entry.getPlayerName() + " was " + entry.getType() + " by " + entry.getStaffName());
    }

    @Override
    public void entryRemoved(Entry entry) {
        // Called when a punishment is removed (unban, unmute, expiry)
        getLogger().info(entry.getPlayerName() + " was unpardoned: " + entry.getType());
    }
});
```

### Event Listener Methods

| Method | When Called |
|---|---|
| `entryAdded(Entry)` | A punishment is issued |
| `entryRemoved(Entry)` | A punishment is removed or expires |
| `entryModified(Entry, String oldReason, String newReason)` | A punishment reason is changed |

---

## Obtaining the API Reference

```java
import dev.kyssta.casualbans.api.CasualBansAPI;

public class MyPlugin extends JavaPlugin {
    private CasualBansAPI casualBansAPI;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("CasualBans") == null) {
            getLogger().severe("CasualBans not found! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.casualBansAPI = CasualBansAPI.getInstance();
        getLogger().info("Hooked into CasualBans API!");
    }
}
```

> Always check that CasualBans is loaded before accessing the API. Safe access pattern shown above.

---

## API Stability

The `casualbans-api` module follows **Semantic Versioning**:

- Breaking changes increment the **major** version.
- Additions increment the **minor** version.
- Patches increment the **patch** version.

The API surface (`CasualBansAPI`, `Database`, `PunishmentManager`, `Events`, `Entry`) is considered stable through 1.x releases.
