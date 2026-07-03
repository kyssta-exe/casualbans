# Importing Data

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans includes a built-in import system to migrate punishments from other plugins. All imports are performed via the `/casualbans import` command.

> **Important:** Run imports on a **single server** with the target database configured in `config.yml`. Imports are additive — existing data is preserved.

---

## Import Sources

| Source | Command | Dependencies |
|---|---|---|
| **LiteBans** | `/casualbans import litebans` | Direct database access (credentials) |
| **AdvancedBan** | `/casualbans import advancedban` | Direct database access (credentials) |
| **Vanilla** | `/casualbans import vanilla` | Access to server's banned-players.json |
| **JSON** | `/casualbans import json <file>` | JSON export file |

---

## Import from LiteBans

Migrate all punishments from a LiteBans database:

```
/casualbans import litebans <host> <database> <username> <password> [port]
```

**Example:**

```
/casualbans import litebans localhost litebans_db root password123 3306
```

This imports:
- All active and expired bans
- All active and expired mutes
- All warnings
- All kick records
- All punishment history
- Staff names are preserved

> LiteBans tables (`litebans_bans`, `litebans_mutes`, `litebans_history`, `litebans_kicks`) are detected automatically. No schema changes needed.

---

## Import from AdvancedBan

Migrate all punishments from an AdvancedBan database:

```
/casualbans import advancedban <host> <database> <username> <password> [port]
```

**Example:**

```
/casualbans import advancedban localhost advancedban_db root password123 3306
```

This imports:
- All active and expired punishments
- All punishment history
- All notes

> AdvancedBan tables (`Punishment`, `PunishmentHistory`, `Notes`) are detected automatically. Both MySQL and H2 backends are supported.

---

## Import from Vanilla

Import vanilla Minecraft server bans (from `banned-players.json` and `banned-ips.json`):

```
/casualbans import vanilla
```

The import reads directly from your server's root directory. No arguments needed.

> Vanilla imports are one-time. Only permanent bans are supported — temp-bans are not part of the vanilla ban system.

---

## Import from JSON

Import punishments from a previously exported JSON file:

```
/casualbans import json <file>
```

**Example:**

```
/casualbans import json plugins/CasualBans/backups/export-2026-06-01.json
```

> The JSON file must follow the CasualBans export schema. Files from older exports or manual edits may fail validation.

---

## Export (Backup)

You can also export data for backup or migration:

```
/casualbans export json
/casualbans export csv
```

Exports are saved to `plugins/CasualBans/backups/` with a timestamp.

---

## Import Progress & Verification

During import, progress is shown in the console:

```
[INFO] [CasualBans] Importing from LiteBans...
[INFO] [CasualBans]   → Bans:     1,247 imported
[INFO] [CasualBans]   → Mutes:      342 imported
[INFO] [CasualBans]   → Warnings:   891 imported
[INFO] [CasualBans]   → Kicks:      156 imported
[INFO] [CasualBans]   → History:  2,183 imported
[INFO] [CasualBans] Import complete in 3.2s.
```

Verify the import with:

```
/casualbans status
/history <player>           # Check a known player's records
/banlist                    # Confirm active bans carried over
```

---

## Rollback an Import

Imports are **not reversible** by default. Always back up your database first:

```
/casualbans backup
```

To undo an import, restore from a backup:

```
/casualbans restore plugins/CasualBans/backups/backup-2026-06-01.json
```
