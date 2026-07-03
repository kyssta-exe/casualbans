# Web Interface

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans includes a built-in web dashboard and REST API — no separate web server or plugin required.

---

## Enabling the Web Interface

```yaml
# config.yml
web:
  enabled: true
  host: 0.0.0.0
  port: 8080
  ssl: false
  authentication: true
  username: admin
  password: ''                    # Auto-generated on first enable
```

1. Set `web.enabled: true` in `config.yml`.
2. Run `/casualbans reload`.
3. The auto-generated password is printed once to console and saved to `config.yml`.
4. Open `http://<your-server-ip>:8080` in your browser.

> Use a reverse proxy (e.g., Nginx) with SSL for production access. Set `web.host` to `127.0.0.1` if proxying locally.

---

## Dashboard

The dashboard is a dark-themed web interface with the CasualBans brand styling:

| Section | Description |
|---|---|
| **Overview** | Live stats — total bans, mutes, warnings, active punishments |
| **Recent Activity** | Chronological feed of the latest punishment actions |
| **Punishment Search** | Search by player name, staff name, IP address, or reason |
| **Player Lookup** | Full punishment history for a specific player |
| **Server Info** | Connected servers, storage backend, uptime |
| **Quick Actions** | Ban, mute, warn, and kick from the web interface (requires authentication) |

### Stats Cards

The dashboard displays summary cards at the top:

- **Active Bans** — Total currently banned players
- **Active Mutes** — Total currently muted players
- **Total Punishments** — All-time punishment count
- **Online Staff** — Currently active staff members (via sync)

---

## REST API

The web server exposes a REST API at `/api/` for programmatic access.

### Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/stats` | Server statistics | No |
| `GET` | `/api/bans` | List active bans (paginated) | No |
| `GET` | `/api/mutes` | List active mutes (paginated) | No |
| `GET` | `/api/check` | Check a player's punishment status | No |
| `POST` | `/api/punish` | Issue a punishment | Yes |
| `POST` | `/api/unpunish` | Remove a punishment | Yes |

### Example — Get Statistics

```http
GET /api/stats HTTP/1.1
Host: your-server:8080
```

```json
{
  "server": "server1",
  "storage": "mysql",
  "active_bans": 12,
  "active_mutes": 5,
  "active_warnings": 23,
  "total_punishments": 1587,
  "uptime_seconds": 284700
}
```

### Example — Check a Player

```http
GET /api/check?player=Notch HTTP/1.1
Host: your-server:8080
```

```json
{
  "player": "Notch",
  "banned": true,
  "ban": {
    "id": "abc123",
    "reason": "Griefing",
    "staff": "Admin",
    "date": "2026-06-15T14:30:00Z",
    "expires": null,
    "permanent": true
  },
  "muted": false,
  "warnings": []
}
```

### Example — Issue a Punishment (Authenticated)

```http
POST /api/punish HTTP/1.1
Host: your-server:8080
Authorization: Basic YWRtaW46eW91cl9wYXNzd29yZA==
Content-Type: application/json

{
  "type": "tempban",
  "player": "Notch",
  "reason": "Cheating",
  "duration": "7d",
  "silent": false
}
```

### Authentication

When `web.authentication` is `true`, API requests require **HTTP Basic Auth** using the credentials from `config.yml`. Most REST clients and libraries support this natively:

```bash
curl -u admin:your_password http://your-server:8080/api/stats
```

---

## Security

- Set a strong password via `config.yml` or use the auto-generated one.
- Bind to `127.0.0.1` and reverse-proxy through Nginx/Caddy for HTTPS.
- Dashboard actions respect permission checks — staff cannot issue punishments they lack node access for.
- Session tokens expire after 24 hours.
