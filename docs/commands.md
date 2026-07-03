# Commands

[BADGE_PAPER] [BADGE_FOLIA]

All commands are prefixed with `/`. Many support **flags** for additional behaviour:

| Flag | Meaning |
|---|---|
| `-s` | **Silent** — broadcast is hidden from players without `casualbans.notify` |
| `-S` | **Extra silent** — nothing is broadcast; only staff with `casualbans.notify.silent` see it |
| `-p` | **Public** — overrides silent defaults; forces a public broadcast |
| `-m <reason>` | **Modify** — changes the reason on an existing punishment |
| `-d` | **Delete** — removes the punishment entirely |
| `-I` | **IP scope** — targets by IP address instead of username |

---

## Punishment Commands

| Command | Aliases | Description |
|---|---|---|
| `/ban <player> [reason]` | — | Permanently ban a player |
| `/tempban <player> <duration> [reason]` | `/tban` | Temporarily ban a player |
| `/ipban <player> [reason]` | `/ip-ban` | Ban the player's IP address |
| `/mute <player> [reason]` | — | Permanently mute a player |
| `/tempmute <player> <duration> [reason]` | `/tmute` | Temporarily mute a player |
| `/ipmute <player> [reason]` | `/ip-mute` | Mute the player's IP address |
| `/warn <player> [reason]` | — | Issue a warning to a player |
| `/kick <player> [reason]` | — | Kick a player from the server |

**Duration format:** `30s` (seconds), `5m` (minutes), `2h` (hours), `7d` (days), `30d` (days), `1y` (years), or combine: `1d6h30m`.

---

## Unpunishment Commands

| Command | Aliases | Description |
|---|---|---|
| `/unban <player>` | `/pardon` | Unban a player |
| `/unmute <player>` | — | Unmute a player |
| `/unwarn <player> [id]` | — | Remove a specific warning by ID, or the latest warning |

### Own Unpunishment

Players with `casualbans.unban.own`, `casualbans.unmute.own`, or `casualbans.unwarn.own` can forgive punishments that **they** issued.

---

## History & Lookup Commands

| Command | Aliases | Description |
|---|---|---|
| `/history <player> [page]` | `/hist`, `/h` | View a player's full punishment history |
| `/staffhistory <staff> [page]` | `/shist` | View punishments issued by a staff member |
| `/checkban <player>` | `/cban` | Check if a player is banned and see details |
| `/checkmute <player>` | `/cmute` | Check if a player is muted and see details |
| `/banlist [page]` | `/blist` | List all active bans |
| `/mutelist [page]` | `/mlist` | List all active mutes |
| `/warnings <player>` | `/warns` | List all active warnings for a player |

---

## Investigation Commands

| Command | Aliases | Description |
|---|---|---|
| `/dupeip <player>` | `/alts` | List all accounts sharing the same IP address |
| `/ipreport <ip>` | — | Show all activity from a specific IP |
| `/iphistory <player>` | — | Show all IP addresses a player has used |
| `/namehistory <player>` | `/names` | Show all past usernames a player has used |
| `/lastsession <player>` | `/lastseen` | Show when a player last logged out |
| `/geoip <player>` | `/geo` | Show geographic data for a player's IP |

---

## Staff Tool Commands

| Command | Aliases | Description |
|---|---|---|
| `/lockdown` | — | Lock down the server — players without `casualbans.lockdown.bypass` cannot join |
| `/staffrollback <staff> [duration]` | `/srollback` | Mass-undo punishments issued by a staff member within a time window |
| `/prunehistory <duration>` | — | Delete punishment records older than the specified duration |
| `/mutechat` | — | Disable chat for players without `casualbans.mutechat.bypass` |
| `/togglechat [player]` | — | Toggle a player's ability to chat on/off |
| `/clearchat [player]` | `/cc` | Clear chat for all players or a specific player |

---

## Note Commands

| Command | Aliases | Description |
|---|---|---|
| `/note <player> <note>` | — | Add a private note to a player's profile |
| `/notelist <player>` | `/notes` | View all notes for a player |
| `/delnote <player> <id>` | — | Delete a specific note by ID |

---

## Plugin Management Commands

| Command | Description |
|---|---|
| `/casualbans reload` | Reload all configuration files without restarting |
| `/casualbans info` | Display plugin version, storage backend, and statistics |
| `/casualbans status` | Detailed health check: storage, sync, webhooks, web interface |
| `/casualbans import <source> ...` | Import punishments from another plugin — see [Import](import.md) |
| `/casualbans allow <player>` | Whitelist a player through a lockdown |
| `/casualbans unlink <player>` | Remove a player's alt-link from the dupeip system |
| `/casualbans backup` | Create a manual backup of the punishment database |
| `/casualbans restore <file>` | Restore punishments from a backup file |
| `/casualbans export <format>` | Export punishments to a portable format (JSON/CSV) |

> **Tip:** Use `/casualbans` with no arguments to show a help summary in-game.
