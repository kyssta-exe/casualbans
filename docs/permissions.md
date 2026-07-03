# Permissions

[BADGE_PAPER] [BADGE_FOLIA]

CasualBans uses a permission-node system compatible with any permissions plugin (LuckPerms, PermissionsEx, GroupManager, etc.).

> **Wildcard:** `casualbans.*` grants every permission listed below.

---

## Punishment Permissions

| Permission | Command(s) |
|---|---|
| `casualbans.ban` | `/ban` |
| `casualbans.tempban` | `/tempban` |
| `casualbans.ipban` | `/ipban` |
| `casualbans.mute` | `/mute` |
| `casualbans.tempmute` | `/tempmute` |
| `casualbans.ipmute` | `/ipmute` |
| `casualbans.warn` | `/warn` |
| `casualbans.kick` | `/kick` |

---

## Unpunishment Permissions

| Permission | Command(s) |
|---|---|
| `casualbans.unban` | `/unban` |
| `casualbans.unmute` | `/unmute` |
| `casualbans.unwarn` | `/unwarn` |

### Own Unpunishments

Allow a staff member to revoke punishments they personally issued:

| Permission | Behaviour |
|---|---|
| `casualbans.unban.own` | Can unban only players they banned |
| `casualbans.unmute.own` | Can unmute only players they muted |
| `casualbans.unwarn.own` | Can remove only warnings they issued |

---

## History & Lookup Permissions

| Permission | Command(s) |
|---|---|
| `casualbans.history` | `/history` |
| `casualbans.staffhistory` | `/staffhistory` |
| `casualbans.checkban` | `/checkban` |
| `casualbans.checkmute` | `/checkmute` |
| `casualbans.banlist` | `/banlist` |
| `casualbans.mutelist` | `/mutelist` |
| `casualbans.warnings` | `/warnings` |
| `casualbans.warnings.self` | `/warnings` — view own warnings only |

---

## Investigation Permissions

| Permission | Command(s) |
|---|---|
| `casualbans.dupeip` | `/dupeip`, `/alts` |
| `casualbans.ipreport` | `/ipreport` |
| `casualbans.iphistory` | `/iphistory` |
| `casualbans.namehistory` | `/namehistory`, `/names` |
| `casualbans.lastsession` | `/lastsession`, `/lastseen` |
| `casualbans.geoip` | `/geoip`, `/geo` |

---

## Staff Tool Permissions

| Permission | Command(s) |
|---|---|
| `casualbans.staffrollback` | `/staffrollback` |
| `casualbans.prunehistory` | `/prunehistory` |
| `casualbans.lockdown` | `/lockdown` |
| `casualbans.lockdown.bypass` | Allows joining during lockdown |
| `casualbans.mutechat` | `/mutechat` |
| `casualbans.togglechat` | `/togglechat` |
| `casualbans.togglechat.bypass` | Chat cannot be toggled off for these players |
| `casualbans.clearchat` | `/clearchat` |
| `casualbans.clearchat.bypass` | Chat is not cleared by `/clearchat` |
| `casualbans.note` | `/note`, `/notelist` |
| `casualbans.note.delete` | `/delnote` |

---

## Admin & Notification Permissions

| Permission | Behaviour |
|---|---|
| `casualbans.admin` | Grants access to all `/casualbans` management commands |
| `casualbans.notify` | Sees silent (`-s`) punishment broadcasts |
| `casualbans.notify.silent` | Sees extra-silent (`-S`) punishment broadcasts |
| `casualbans.notify.broadcast` | Receives real-time punishment broadcasts |
| `casualbans.notify.bannedjoin` | Notified when a banned player attempts to join |
| `casualbans.notify.mutedchat` | Notified when a muted player attempts to chat |

---

## Exemption Permissions

Players with these permissions are exempt from specific punishment types:

| Permission | Behaviour |
|---|---|
| `casualbans.exempt` | Immune to all punishment types |
| `casualbans.exempt.ban` | Immune to bans |
| `casualbans.exempt.mute` | Immune to mutes |
| `casualbans.exempt.warn` | Immune to warnings |
| `casualbans.exempt.kick` | Immune to kicks |
| `casualbans.exempt.bypass` | Bypasses all exemption checks (overrides exempt) |

---

## Special Permissions

| Permission | Behaviour |
|---|---|
| `casualbans.group.unlimited` | No duration limit on punishments this staff member issues |
| `casualbans.cooldown.bypass` | Ignores command cooldowns set in `config.yml` |

---

## Flag Permissions

| Permission | Flag | Behaviour |
|---|---|---|
| `casualbans.silent` | `-s` | Can issue silent punishments |
| `casualbans.extrasilent` | `-S` | Can issue extra-silent punishments |
| `casualbans.public` | `-p` | Can force a public broadcast on normally silent commands |
| `casualbans.modify` | `-m` | Can modify any punishment reason |
| `casualbans.modify.own` | `-m` | Can modify only own punishment reasons |
| `casualbans.delete` | `-d` | Can delete any punishment entirely |
| `casualbans.delete.own` | `-d` | Can delete only own punishments |
| `casualbans.server.global` | — | Punishments apply across all synced servers |

---

## Setup Example (LuckPerms)

```
/lp group admin permission set casualbans.admin true
/lp group admin permission set casualbans.exempt.bypass true
/lp group admin permission set casualbans.notify.silent true
/lp group admin permission set casualbans.lockdown.bypass true
/lp group mod permission set casualbans.tempban true
/lp group mod permission set casualbans.mute true
/lp group mod permission set casualbans.warn true
/lp group mod permission set casualbans.history true
/lp group mod permission set casualbans.kick true
/lp group mod permission set casualbans.unban.own true
/lp group mod permission set casualbans.unmute.own true
/lp group mod permission set casualbans.unwarn.own true
/lp group mod permission set casualbans.notify true
/lp group mod permission set casualbans.silent true
```
