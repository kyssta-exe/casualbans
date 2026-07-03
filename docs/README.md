# CasualBans Documentation

[BADGE_PAPER] [BADGE_FOLIA] [BADGE_JAVA] [BADGE_MIT]

CasualBans is a modern, all-in-one punishment management plugin for Paper 1.21–26.1.2 servers. It replaces LiteBans, AdvancedBan, and similar plugins with a single, performant, cross-server-ready solution.

## Features

- **8 Punishment Types** — Ban, TempBan, IP-Ban, Mute, TempMute, IP-Mute, Warn, Kick
- **Cross-Server Sync** — Punishments, broadcasts, and notifications sync across all servers sharing a database
- **3 Storage Backends** — JSON (default), H2, MySQL, MariaDB, PostgreSQL
- **Template System** — Ladder-based punishment escalation for repeat offenders with weighted template groups
- **Staff Tools** — Staff rollback, history pruning, lockdown, mutechat, clearchat, notes
- **Investigation** — DupeIP alt detection, IP history, name history, last session, GeoIP
- **Discord Webhooks** — Real-time punishment, alt detection, and staff action notifications
- **Built-in Web Dashboard** — Dark-themed web interface with live stats, search, and REST API
- **Plugin Integration** — PlaceholderAPI, ProtocolLib, Vault, LuckPerms
- **Developer API** — Full `StorageProvider` interface, `PunishmentManager` for programmatic control
- **Import System** — Migrate from LiteBans, AdvancedBan, Vanilla, or JSON
- **Folia Support** — Fully compatible with Folia's regionized threading
- **Command Flags** — `-s` (silent), `-S` (extra silent), `-p` (public), `-m` (modify), `-d` (delete), `-I` (IP)
- **Staff Groups** — Configurable permission-based groups with cooldowns, duration limits, and weight hierarchy
- **Messages** — Fully customizable MiniMessage format with gradients and hex colors
- **Minimal Dependencies** — Lombok, HikariCP, Gson, H2/MySQL/PostgreSQL drivers (all bundled)

## Quick Links

| Topic | Link |
|-------|------|
| Installation & Setup | [installation.md](installation.md) |
| All Commands | [commands.md](commands.md) |
| Permission Nodes | [permissions.md](permissions.md) |
| Configuration Files | [configuration.md](configuration.md) |
| Storage Setup | [storage.md](storage.md) |
| Discord Webhooks | [webhooks.md](webhooks.md) |
| Web Dashboard & API | [web-interface.md](web-interface.md) |
| Developer API | [api.md](api.md) |
| Importing Data | [import.md](import.md) |
| FAQ | [faq.md](faq.md) |

## Need Help?

- **Issues:** [GitHub Issues](https://github.com/kyssta-exe/casualbans/issues)
