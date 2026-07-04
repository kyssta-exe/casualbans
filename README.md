<div align="center">

# CasualBans

**A modern, feature-rich cross-server punishment management system for Paper 1.21+**

![Paper](https://img.shields.io/badge/Paper-1.21--26.1.2-8c75a5?style=flat-square&logo=minecraft)
![Folia](https://img.shields.io/badge/Folia-✔-468ac9?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-46c98a?style=flat-square&logo=openjdk)
![License](https://img.shields.io/github/license/kyssta-exe/casualbans?style=flat-square&color=c98a46)
![Version](https://img.shields.io/github/v/release/kyssta-exe/casualbans?style=flat-square&color=8c75a5)

[Features](#features) • [Quick Start](#quick-start) • [Commands](#commands) • [Configuration](#configuration) • [Building](#building) • [Documentation](#documentation)

</div>

---

CasualBans is the modern replacement for LiteBans, AdvancedBan, and similar punishment plugins. It's built from the ground up for Paper 1.21+ with cross-server sync, a powerful template system, Discord integration, and a clean developer API — all in a single zero-dependency JAR.

## Features

- **8 Punishment Types** — Ban, TempBan, IP-Ban, Mute, TempMute, IP-Mute, Warn, Kick
- **Cross-Server Sync** — Punishments, broadcasts, and notifications sync across all servers sharing a database
- **3 Storage Backends** — JSON (default), H2, MySQL, MariaDB, PostgreSQL
- **Template System** — Ladder-based punishment escalation with weighted template groups
- **Staff Tools** — Rollback, history pruning, lockdown, mutechat, clearchat, staff notes
- **Investigation Tools** — DupeIP alt detection, IP history, name history, last session, GeoIP
- **Discord Webhooks** — Real-time notifications for punishments, alts, and staff actions
- **Built-in Web Dashboard** — Dark-themed UI with live stats, search, and REST API
- **Plugin Integrations** — PlaceholderAPI, ProtocolLib, Vault, LuckPerms
- **Developer API** — Full `StorageProvider` + `PunishmentManager` for programmatic control
- **Import System** — Migrate from LiteBans, AdvancedBan, Vanilla, or JSON
- **Folia Support** — Fully compatible with regionized threading
- **Command Flags** — `-s` silent, `-S` extra silent, `-p` public, `-m` modify, `-d` delete, `-I` IP
- **Staff Groups** — Configurable groups with cooldowns, duration limits, and weight hierarchy
- **Customizable Messages** — MiniMessage format with hex colors and gradients
- **Zero Runtime Dependencies** — HikariCP, Gson, H2, MySQL, PostgreSQL drivers all bundled

## Quick Start

```bash
# 1. Download the latest release
# 2. Place CasualBans-<version>.jar in your plugins/ folder
# 3. Start the server (config files auto-generate)
# 4. Configure storage and features
# 5. Start moderating
```

```yaml
# plugins/CasualBans/config.yml (essential settings)
server:
  name: 'my-server'
  default-scope: '*'
storage:
  driver: JSON  # or MYSQL, MARIADB, POSTGRESQL, H2
```

## Commands

| Command | Description | Permission |
|---------|-------------|-----------|
| `/ban <player> [duration] [reason]` | Ban a player | `casualbans.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily ban | `casualbans.tempban` |
| `/ipban <player> [duration] [reason]` | IP ban | `casualbans.ipban` |
| `/mute <player> [duration] [reason]` | Mute a player | `casualbans.mute` |
| `/tempmute <player> <duration> [reason]` | Temporarily mute | `casualbans.tempmute` |
| `/warn <player> [reason]` | Warn a player | `casualbans.warn` |
| `/kick <player> [reason]` | Kick a player | `casualbans.kick` |
| `/unban <player> [reason]` | Unban a player | `casualbans.unban` |
| `/unmute <player> [reason]` | Unmute a player | `casualbans.unmute` |
| `/history <player> [type] [limit]` | View punishment history | `casualbans.history` |
| `/checkban <player>` | Check ban status | `casualbans.checkban` |
| `/banlist [page]` | List active bans | `casualbans.banlist` |
| `/dupeip <player>` | Find alt accounts | `casualbans.dupeip` |
| `/lockdown [reason\|end]` | Server lockdown | `casualbans.lockdown` |
| `/mutechat [-s]` | Toggle chat | `casualbans.mutechat` |
| `/casualbans <reload\|import\|info>` | Admin commands | `casualbans.admin` |

## Permissions Overview

| Category | Key Permissions |
|----------|----------------|
| Punishment | `casualbans.ban`, `.tempban`, `.ipban`, `.mute`, `.tempmute`, `.warn`, `.kick` |
| Removal | `casualbans.unban`, `.unmute`, `.unwarn` (and `.own` variants) |
| Investigation | `casualbans.history`, `.checkban`, `.banlist`, `.dupeip`, `.iphistory`, `.geoip` |
| Staff Tools | `casualbans.lockdown`, `.mutechat`, `.clearchat`, `.staffrollback`, `.note` |
| Notifications | `casualbans.notify`, `.notify.silent`, `.notify.broadcast` |
| Exempt | `casualbans.exempt`, `.exempt.ban`, `.exempt.mute` |
| Flags | `casualbans.silent`, `.extrasilent`, `.public`, `.modify`, `.delete` |
| Admin | `casualbans.admin` |

## Configuration

```yaml
# plugins/CasualBans/config.yml (abbreviated)
server:
  name: 'casualbans'
  default-scope: '*'

storage:
  driver: JSON
  sql:
    host: localhost
    port: 3306
    database: casualbans
    username: root
    password: ''

mutes:
  enabled: true
  command-blacklist: ['/me', '/say', '/msg']

warnings:
  expire-after: 30d
  actions:
    - '3:kick $player Final warning: $reason'
    - '4:tempban $player 7d Reached 4 warnings'

durations:
  limits:
    admin:
      permission: casualbans.group.admin
      tempban: 6 months
      cooldown-ban: 0s
      weight: 100

webhooks:
  enabled: false
  punishment-url: ''
  alt-url: ''
  staff-url: ''

web:
  enabled: false
  port: 8291
```

## Building

**Requirements:** Java 21+, Git

```bash
git clone https://github.com/kyssta-exe/casualbans.git
cd CasualBans
./gradlew shadowJar
```

The compiled JAR is at `build/libs/CasualBans-1.0.0.jar` — includes all dependencies.

## Documentation

Full documentation is available at **[https://kyssta-exe.github.io/CasualBans](https://kyssta-exe.github.io/CasualBans)**

| Page | Description |
|------|-------------|
| [Installation](docs/installation.md) | Requirements, build, first setup |
| [Commands](docs/commands.md) | Full command reference |
| [Permissions](docs/permissions.md) | All permission nodes |
| [Configuration](docs/configuration.md) | config.yml, messages.yml, templates.yml |
| [Storage](docs/storage.md) | JSON vs SQL setup |
| [Webhooks](docs/webhooks.md) | Discord webhook setup |
| [Web Interface](docs/web-interface.md) | Dashboard & REST API |
| [Developer API](docs/api.md) | Programmatic integration |
| [Importing](docs/import.md) | Migrate from LiteBans, AdvancedBan, etc. |
| [FAQ](docs/faq.md) | Common questions |

## Built With

- [Paper API](https://papermc.io) — Minecraft server API
- [HikariCP](https://github.com/brettwooldridge/HikariCP) — Connection pooling
- [Gson](https://github.com/google/gson) — JSON serialization
- [MySQL Connector](https://dev.mysql.com/downloads/connector/j/) — MySQL driver
- [PostgreSQL JDBC](https://jdbc.postgresql.org/) — PostgreSQL driver
- [H2 Database](https://www.h2database.com) — Embedded SQL
- [Lombok](https://projectlombok.org) — Boilerplate reduction
- [GeoIP2](https://github.com/maxmind/GeoIP2-java) — IP geolocation
- [Adventure](https://docs.advntr.dev) — Modern Minecraft text API

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Support

- [GitHub Issues](https://github.com/kyssta-exe/casualbans/issues) — Bug reports & feature requests
