# Installation

[BADGE_PAPER] [BADGE_FOLIA] [BADGE_JAVA]

## Requirements

| Dependency | Minimum Version |
|---|---|
| **Server Software** | [Paper](https://papermc.io) 1.21+ or [Folia](https://papermc.io/software/folia) 1.21+ |
| **Java** | Java 21 or newer |
| **Database** (optional) | MySQL 8.0+, MariaDB 10.6+, or PostgreSQL 14+ |
| **Dependencies** | All bundled — no additional plugins required |

> CasualBans has **zero hard runtime dependencies**. PlaceholderAPI, Vault, ProtocolLib, and LuckPerms are detected at runtime if present for enhanced functionality.

## Step 1 — Download

Grab the latest `CasualBans.jar` from:

- **[GitHub Releases](https://github.com/kyssta-exe/casualbans/releases)** — Recommended
- **[Modrinth](https://modrinth.com/plugin/casualbans)** — Alternative mirror

## Step 2 — Install

1. Place the `CasualBans.jar` file into your server's `plugins/` directory.
2. Restart (or `/reload confirm`) your server.

On first launch, CasualBans automatically:
- Creates `plugins/CasualBans/config.yml` with default values
- Creates `plugins/CasualBans/messages.yml` with all user-facing messages
- Creates `plugins/CasualBans/templates.yml` with default punishment escalation ladders
- Initialises the **JSON** storage backend (no database setup required)

## Step 3 — Verify

Run `/casualbans status` in-game or from console. A successful install shows:

```
[INFO] [CasualBans] Server connected to storage [JSON] — 0 bans, 0 mutes on record
[INFO] [CasualBans] Webhooks disabled | Web interface disabled
```

## Quick Start

Once the plugin is running, set up your permissions:

1. **Assign yourself admin:** `/lp user <you> permission set casualbans.admin true`
2. **Test a punishment:** `/warn <player> Test punishment — please ignore`
3. **Check the history:** `/history <player>`
4. **Configure storage** (if using MySQL): Edit `plugins/CasualBans/config.yml` → `storage` section → restart

See the [Quick Start sidebar link](installation.md#quick-start) for a condensed checklist:

- [x] JAR installed and server restarted
- [ ] Permissions granted to staff members
- [ ] **Optional:** Database configured for cross-server sync
- [ ] **Optional:** Discord webhooks set up in `config.yml`
- [ ] **Optional:** Web interface enabled in `config.yml`

## Updating

1. Stop the server.
2. Replace `plugins/CasualBans/CasualBans.jar` with the new version.
3. Start the server.

> Configuration files are automatically migrated on startup. Backups of the old config are saved as `config.yml.bak`.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Plugin doesn't load | Java version < 21 | Run `java -version` and update to Java 21+ |
| "Unsupported API version" | Server software incompatible | Use Paper 1.21+ or Folia |
| Commands not working | Permissions not set | Grant `casualbans.*` or the specific permission node |
| Storage errors | Database credentials wrong | Check `config.yml` → `storage` section |
