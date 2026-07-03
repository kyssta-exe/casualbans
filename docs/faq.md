# FAQ

[BADGE_PAPER] [BADGE_FOLIA]

## General

### What is CasualBans?

CasualBans is a modern, all-in-one punishment management plugin for Paper 1.21+ servers. It replaces standalone plugins like LiteBans, AdvancedBan, and BanManager with a single, performant, cross-server-ready solution.

### Is CasualBans free?

Yes. CasualBans is open-source under the **MIT license** — free to use, modify, and distribute.

### What server software is supported?

Paper 1.21+ and Folia. Spigot, Purpur, and other Paper forks are also supported. Vanilla servers are **not** supported.

### Does CasualBans work with Folia?

Yes. CasualBans is fully compatible with Folia's regionised threading model. All storage and punishment operations run asynchronously.

---

## Installation & Setup

### Do I need a database?

No. CasualBans uses **JSON storage** by default — just drop the JAR and start the server. Switch to MySQL/MariaDB/PostgreSQL when you need cross-server sync.

### How do I set up MySQL?

Create a database and user, then configure the `storage` section in `config.yml`. See the [Storage](storage.md) page for exact steps.

### Can I use an existing database from LiteBans?

Yes. Use the [import system](import.md) to migrate all data from LiteBans, AdvancedBan, or vanilla.

### Do I need other plugins?

No. All dependencies (HikariCP, Gson, database drivers) are bundled. PlaceholderAPI, Vault, ProtocolLib, and LuckPerms are optional but enhance functionality when present.

---

## Commands & Permissions

### How do I give someone access to all commands?

```
/lp user <name> permission set casualbans.admin true
```

Or use the wildcard: `casualbans.*`. See [Permissions](permissions.md) for detailed node listings.

### What does the `-s` flag do?

`-s` makes a punishment **silent** — the broadcast is only visible to players with `casualbans.notify`. Use `-S` for extra-silent (only `casualbans.notify.silent`).

### Why can't a staff member unban someone?

They may only have `casualbans.unban.own`, which restricts unbans to players they personally banned. Grant `casualbans.unban` for full access.

### Can players see their own warnings?

Yes, if they have `casualbans.warnings.self`. They can run `/warnings` to view only their own active warnings.

---

## Configuration

### How do I change what messages say?

Edit `plugins/CasualBans/messages.yml`. Messages use the **MiniMessage** format — supports gradients, hex colours, hover text, and click events.

### How do I reload config without restarting?

Run `/casualbans reload`. This reloads all three config files (config.yml, messages.yml, templates.yml) without restarting the server.

### Can I set different cooldowns per command?

Yes. In `config.yml` under `security.cooldown.per-command`:

```yaml
security:
  cooldown:
    per-command:
      /ban: 5000
      /mute: 3000
      /warn: 1000
```

---

## Storage & Data

### How do I switch from JSON to MySQL?

1. Configure the `storage` section in `config.yml` to use `mysql`.
2. Run `/casualbans reload`.
3. Use the [import system](import.md) to migrate data from JSON to MySQL.

> CasualBans does not auto-migrate between backends. Always use the export/import workflow.

### Where is the JSON data stored?

In `plugins/CasualBans/data/punishments.json`.

### Can I set a table prefix?

Yes. Set `storage.table-prefix` in `config.yml`. Example: `cb_` creates `cb_punishments`, `cb_history`, etc.

### How do I back up my data?

Run `/casualbans backup`, or export with `/casualbans export json`. Backups are saved to `plugins/CasualBans/backups/`.

---

## Cross-Server

### How does cross-server sync work?

Servers share the same database. When a punishment is issued on one server, it's broadcast to all other connected servers via database polling. Sync settings are in `config.yml` → `sync`.

### Do all servers need the same config?

Not necessarily. Each server has its own `server-name` and `server-group` in config.yml. Punishments can be targeted globally or per-group.

---

## Web Interface

### How do I enable the web dashboard?

Set `web.enabled: true` in `config.yml` and reload. The password is auto-generated on first enable. See [Web Interface](web-interface.md).

### Is the web API authenticated?

Yes. HTTP Basic Auth is enabled by default. Disable it by setting `web.authentication: false` — not recommended for public-facing setups.

### Can I use HTTPS?

CasualBans supports SSL natively (`web.ssl: true`), but a reverse proxy (Nginx with Certbot) is recommended for production.

---

## Troubleshooting

### The plugin didn't load. What's wrong?

Check the server log for errors. Common causes:
- Java version below 21
- Running on CraftBukkit/vanilla (use Paper)
- JAR file corrupted (re-download and replace)

### Webhooks aren't working.

Run `/casualbans status` to check connectivity. Verify the webhook URL is correct and the Discord channel exists. Ensure `webhooks.enabled: true` in config.yml.

### A command isn't working.

Check permission nodes with `/lp user <name> permission check casualbans.<command>`. See [Permissions](permissions.md) for the exact node name.

### The web interface shows "Connection refused".

Ensure `web.enabled: true` and that the port isn't blocked by a firewall. If behind a reverse proxy, verify proxy configuration.

---

## Support

### Where do I report bugs?

Open an issue on [GitHub Issues](https://github.com/Kyssta/CasualBans/issues).

### Where can I get help?

Join the [Kyssta Discord](https://discord.gg/kyssta) for community support and announcements.

### Can I contribute?

Absolutely! Fork the repo, make your changes, and open a pull request on [GitHub](https://github.com/Kyssta/CasualBans). All contributions are welcome.
