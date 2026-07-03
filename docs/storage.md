# Storage

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans supports multiple storage backends. By default it uses **JSON** — zero setup required.

---

## Backends Overview

| Backend | ID | Setup Required | Cross-Server Sync | Performance |
|---|---|---|---|---|
| JSON | `json` | ❌ None | ❌ | ⭐⭐ |
| H2 | `h2` | ❌ Auto-created | ❌ | ⭐⭐⭐ |
| MySQL | `mysql` | ✅ Create database | ✅ | ⭐⭐⭐⭐ |
| MariaDB | `mariadb` | ✅ Create database | ✅ | ⭐⭐⭐⭐ |
| PostgreSQL | `postgresql` | ✅ Create database | ✅ | ⭐⭐⭐⭐⭐ |

---

## JSON (Default)

The JSON backend stores all data in a single `punishments.json` file inside `plugins/CasualBans/data/`.

```yaml
# config.yml
storage:
  backend: json
```

**Pros:** Zero configuration, portable, human-readable.
**Cons:** No cross-server sync, slower at scale, no query capabilities.

> JSON is ideal for **single-server** setups and testing. Switch to a database backend for production networks.

---

## H2 (Embedded SQL)

H2 is an embedded SQL database — no external server needed. The file is stored as `plugins/CasualBans/data/database.mv.db`.

```yaml
# config.yml
storage:
  backend: h2
```

**Pros:** Full SQL query support, faster than JSON at scale, zero setup.
**Cons:** No cross-server sync, file-based.

---

## MySQL / MariaDB

Full SQL database backends for production networks. Supports cross-server punishment synchronisation.

```yaml
# config.yml
storage:
  backend: mysql            # or mariadb
  table-prefix: cb_         # optional prefix for all tables
  credentials:
    host: localhost
    port: 3306
    database: casualbans
    username: casualbans
    password: your_password
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 5000
    idle-timeout: 300000
    max-lifetime: 600000
```

### Setting Up MySQL

```sql
CREATE DATABASE IF NOT EXISTS casualbans CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'casualbans'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON casualbans.* TO 'casualbans'@'%';
FLUSH PRIVILEGES;
```

Tables are created **automatically** on first connection. No schema import required.

---

## PostgreSQL

High-performance backend for large-scale networks.

```yaml
# config.yml
storage:
  backend: postgresql
  table-prefix: cb_
  credentials:
    host: localhost
    port: 5432
    database: casualbans
    username: casualbans
    password: your_password
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
```

### Setting Up PostgreSQL

```sql
CREATE DATABASE casualbans;
CREATE USER casualbans WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE casualbans TO casualbans;
```

---

## Connection Pooling

All SQL backends use **HikariCP** under the hood for connection pooling. Default settings:

| Setting | Default | Description |
|---|---|---|
| `maximum-pool-size` | 10 | Max concurrent connections |
| `minimum-idle` | 2 | Minimum idle connections to maintain |
| `connection-timeout` | 5000 ms | Timeout for acquiring a connection |
| `idle-timeout` | 300000 ms | Max time a connection stays idle (5 min) |
| `max-lifetime` | 600000 ms | Max lifetime of a connection (10 min) |

---

## Table Prefix

Configure a prefix for all database tables to avoid conflicts with other plugins:

```yaml
storage:
  table-prefix: cb_
```

With the prefix `cb_`, tables are created as `cb_punishments`, `cb_history`, `cb_notes`, etc. Applies to H2, MySQL, MariaDB, and PostgreSQL backends only.

---

## Switching Backends

1. Update `config.yml` with the new backend settings.
2. Run `/casualbans reload`.
3. Existing data stays in the old backend — use the [Import](import.md) system to migrate.

> **Warning:** CasualBans does not auto-migrate data between backends. Use the export/import workflow to move data safely.
