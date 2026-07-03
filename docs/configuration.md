# Configuration

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans ships with three configuration files, all located in `plugins/CasualBans/`. They use **YAML** format and are hot-reloadable via `/casualbans reload`.

| File | Purpose |
|---|---|
| `config.yml` | Core plugin settings — storage, sync, features, webhooks |
| `messages.yml` | All user-facing messages in MiniMessage format |
| `templates.yml` | Punishment template ladders with progressive discipline |

---

## config.yml

The main configuration file is organised into these sections:

```yaml
# ─── Server ───────────────────────────────────────────────
server:
  server-name: 'server1'        # Identifies this server in sync
  server-group: 'default'        # Group for targeted punishment sync
  language: 'en'                 # Message locale

# ─── Storage ──────────────────────────────────────────────
storage:
  backend: 'json'                # json | h2 | mysql | mariadb | postgresql
  table-prefix: 'cb_'            # Prefix for all database tables
  pool:
    maximum-pool-size: 10        # HikariCP max connections
    minimum-idle: 2              # HikariCP idle connections
    connection-timeout: 5000     # ms
    idle-timeout: 300000         # ms
    max-lifetime: 600000         # ms
  credentials:                   # Required for mysql/mariadb/postgresql
    host: 'localhost'
    port: 3306
    database: 'casualbans'
    username: 'root'
    password: ''

# ─── Sync ─────────────────────────────────────────────────
sync:
  enabled: true                  # Cross-server punishment sync
  broadcast: true                # Sync broadcasts to other servers

# ─── Mutes ────────────────────────────────────────────────
mutes:
  prevent-chat: true             # Block chat messages when muted
  prevent-commands: true         # Block command execution when muted
  allowed-commands: []           # Commands allowed while muted (e.g., [/help, /msg])
  cancel-commands: []            # Commands silently cancelled while muted

# ─── Warnings ─────────────────────────────────────────────
warnings:
  max-active: 10                 # Max active warnings before auto-action
  auto-action: 'ban'             # Action when max warnings exceeded
  auto-action-duration: '7d'     # Duration for auto-action (blank=permanent)
  reset-on-punish: true          # Clear warnings when player is punished

# ─── Durations ────────────────────────────────────────────
durations:
  max-ban: '365d'                # Maximum ban length staff can set
  max-mute: '365d'               # Maximum mute length staff can set
  max-tempban: '365d'            # Maximum temp-ban length
  default-ban: '7d'              # Default ban length when not specified
  default-mute: '7d'             # Default mute length when not specified

# ─── Exempt ───────────────────────────────────────────────
exempt:
  permission-bypass: false       # Allow exempt players to bypass permission checks
  max-exempt-duration: '30d'     # Max duration for punishments on exempt players

# ─── GeoIP ────────────────────────────────────────────────
geoip:
  enabled: false                 # Enable GeoIP lookups
  provider: 'maxmind'            # GeoIP provider
  database: 'plugins/CasualBans/GeoLite2-City.mmdb'

# ─── Notifications ────────────────────────────────────────
notifications:
  broadcast-enabled: true        # Broadcast punishments in chat
  broadcast-console: true        # Log punishments to console

# ─── Security ─────────────────────────────────────────────
security:
  cooldown:
    enabled: true
    base: 1000                   # Base cooldown in ms
    per-command: {}              # Per-command overrides, e.g. /ban: 5000
  rate-limit:
    enabled: true
    max-actions: 5               # Max punishments per 10s window
    window: 10000                # Window in ms
    action: 'warn'               # warn | kick | ban (action on rate-limit hit)

# ─── Lockdown ─────────────────────────────────────────────
lockdown:
  kick-message: '&cServer is in lockdown mode.'
  deny-login-message: '&cServer is currently locked down.'

# ─── Display ──────────────────────────────────────────────
display:
  date-format: 'yyyy-MM-dd HH:mm:ss'
  timezone: 'UTC'
  date-relative: true            # Show relative times ("2h ago") in lookups

# ─── History ──────────────────────────────────────────────
history:
  results-per-page: 10           # Pagination size for /history
  max-entries: 10000             # Max stored entries per player (0 = unlimited)

# ─── Performance ──────────────────────────────────────────
performance:
  async-loading: true            # Load player data asynchronously
  cache:
    enabled: true
    max-size: 1000               # Max cached player records
    expire-after: 300000         # Cache TTL in ms

# ─── Integrations ─────────────────────────────────────────
integrations:
  placeholderapi: true           # Enable PAPI placeholders
  vault: true                    # Enable Vault chat/economy hooks
  protocolib: true               # Enable ProtocolLib packet hooks
  luckperms: true                # Enable LuckPerms context integration

# ─── Web ──────────────────────────────────────────────────
web:
  enabled: false                 # Enable the built-in web dashboard
  host: '0.0.0.0'
  port: 8080
  ssl: false
  authentication: true
  username: 'admin'
  password: ''                   # Auto-generated on first enable

# ─── Webhooks ─────────────────────────────────────────────
webhooks:
  enabled: false
  discord:                       # Discord webhook URLs by channel
    punishments: ''
    alerts: ''
    staff: ''
```

> **Note:** Default values are always applied for missing keys — you can safely delete sections you don't need.

---

## messages.yml

All user-facing messages use the **MiniMessage** format (rich colours, gradients, hover/click events).

```yaml
# Example — messages.yml
prefix: '<gradient:#8c75a5:#f46c90>CasualBans</gradient> <dark_gray>»</dark_gray>'
ban-broadcast: |
  <red>⛔ <bold>%player%</bold> was banned by %staff%</red>
  <gray>Reason: <white>%reason%</white></gray>
mute-broadcast: |
  <yellow>🔇 <bold>%player%</bold> was muted by %staff%</yellow>
```

Key placeholders:

| Placeholder | Description |
|---|---|
| `%player%` | The target player's name |
| `%staff%` | The staff member's name |
| `%reason%` | The punishment reason |
| `%duration%` | The punishment duration |
| `%id%` | The punishment record ID |
| `%server%` | The originating server name |

---

## templates.yml

Template ladders automate progressive discipline. Each template has a weight, duration, and reason.

```yaml
# Example — templates.yml
templates:
  spam:
    display-name: '<red>Spam</red>'
    group-weight: 1
    punishments:
      - type: warn
        weight: 1
        reason: 'Please do not spam.'
      - type: mute
        weight: 3
        duration: '1h'
        reason: 'Repeated spamming — 1 hour mute.'
      - type: tempban
        weight: 5
        duration: '7d'
        reason: 'Continued spamming — 7 day ban.'
```

Templates auto-advance through the ladder based on the player's active warning count within that template group, enabling consistent, escalation-based moderation.
