# Webhooks (Discord)

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans can send real-time notifications to **Discord** channels via webhooks. Each webhook is a fully customisable embed with colour coding by punishment type.

---

## Enabling Webhooks

```yaml
# config.yml
webhooks:
  enabled: true
  discord:
    punishments: 'https://discord.com/api/webhooks/...'   # Punishment actions
    alerts:      'https://discord.com/api/webhooks/...'   # Alt detection & security
    staff:       'https://discord.com/api/webhooks/...'   # Staff actions & rollbacks
```

1. Create a webhook in your Discord server: **Server Settings → Integrations → Webhooks → New Webhook**.
2. Copy the webhook URL and paste it into the appropriate field in `config.yml`.
3. Run `/casualbans reload`.

> You can use the same URL for multiple channels, or set up separate webhooks for each category.

---

## Punishment Embeds

When a punishment is issued, CasualBans sends a rich embed to the configured `punishments` webhook URL:

| Punishment Type | Embed Colour | Icon |
|---|---|---|
| Ban / TempBan | `#f46c90` (Pink) | ⛔ |
| IP-Ban | `#c0392b` (Red) | 🚫 |
| Mute / TempMute | `#8c75a5` (Purple) | 🔇 |
| IP-Mute | `#6c3483` (Dark Purple) | 🔈 |
| Warn | `#f39c12` (Amber) | ⚠️ |
| Kick | `#e67e22` (Orange) | 👢 |
| Unban / Unmute / Unwarn | `#2ecc71` (Green) | ✅ |

### Embed Fields

Every punishment embed includes:

| Field | Content |
|---|---|
| **Player** | Username (hover: UUID) |
| **Staff** | Username (hover: UUID) |
| **Reason** | Punishment reason text |
| **Duration** | Duration or `Permanent` |
| **Server** | Originating server name |
| **Date** | Timestamp of the action |
| **ID** | Internal punishment record ID |

---

## Alert Embeds

Sent to the `alerts` webhook URL for security events:

| Event | Embed Colour | Description |
|---|---|---|
| Alt Detection | `#e74c3c` (Red) | Multiple accounts sharing the same IP |
| Banned Player Join Attempt | `#f46c90` (Pink) | A banned player tried to join |
| Muted Player Chat Attempt | `#8c75a5` (Purple) | A muted player tried to speak |
| GeoIP Alert | `#f39c12` (Amber) | Suspicious geographic location |

---

## Staff Action Embeds

Sent to the `staff` webhook URL for moderation operations:

| Action | Embed Colour | Description |
|---|---|---|
| Staff Rollback | `#e74c3c` (Red) | Mass punishment reversal |
| History Prune | `#f39c12` (Amber) | Old records deleted |
| Lockdown Toggle | `#e67e22` (Orange) | Server lockdown enabled/disabled |
| Note Added | `#3498db` (Blue) | Staff note on player profile |

---

## Embed Customisation

Embed appearance (colours, titles, descriptions, footer) can be fully customised via `messages.yml` under the `webhooks` section:

```yaml
messages.yml
webhooks:
  punishment:
    title: '<red>⛔ Punishment Issued</red>'
    color: '#f46c90'
    footer: 'CasualBans • %server%'
    timestamp: true
```

---

## Testing Webhooks

Run `/casualbans status` to verify webhook connectivity:

```
[INFO] [CasualBans] Webhooks: ENABLED
[INFO] [CasualBans]   → Punishments: ✓ Connected
[INFO] [CasualBans]   → Alerts:      ✓ Connected
[INFO] [CasualBans]   → Staff:       ✗ Not configured
```

Issue a test warn to yourself (`/warn <you> Test webhook`) to confirm delivery.
