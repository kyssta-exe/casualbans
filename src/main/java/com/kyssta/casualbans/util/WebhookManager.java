package com.kyssta.casualbans.util;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.manager.InvestigationManager.AccountInfo;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Sends Discord webhook messages for punishments, alt detections, etc.
 * All gated by config.yml webhooks section.
 */
public class WebhookManager {

    private final CasualBans plugin;
    private final String punishmentUrl;
    private final String altUrl;
    private final String staffUrl;
    private final boolean enabled;

    public WebhookManager(CasualBans plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("webhooks.enabled", false);
        this.punishmentUrl = plugin.getConfig().getString("webhooks.punishment-url", "");
        this.altUrl = plugin.getConfig().getString("webhooks.alt-url", "");
        this.staffUrl = plugin.getConfig().getString("webhooks.staff-url", "");
    }

    public boolean isEnabled() { return enabled; }
    public String getAltUrl() { return altUrl; }

    public void sendPunishmentWebhook(Punishment punishment) {
        if (!enabled || punishmentUrl.isEmpty()) return;
        String title;
        int color;

        switch (punishment.getType()) {
            case BAN, TEMPBAN -> { title = "🚫 Ban"; color = 0xED4245; }
            case IPBAN -> { title = "🚫 IP Ban"; color = 0xED4245; }
            case MUTE, TEMPMUTE -> { title = "🔇 Mute"; color = 0xFEE75C; }
            case IPMUTE -> { title = "🔇 IP Mute"; color = 0xFEE75C; }
            case WARN -> { title = "⚠️ Warning"; color = 0xFEE75C; }
            case KICK -> { title = "👢 Kick"; color = 0x5865F2; }
            case UNBAN -> { title = "✅ Unban"; color = 0x57F287; }
            case UNMUTE -> { title = "✅ Unmute"; color = 0x57F287; }
            case UNWARN -> { title = "✅ Warning Removed"; color = 0x57F287; }
            default -> { title = "Punishment"; color = 0x5865F2; }
        }

        Embed embed = new Embed()
            .title(title)
            .color(color)
            .field("Player", punishment.getName() != null ? punishment.getName() : "Unknown", true)
            .field("Executor", punishment.getExecutorName(), true)
            .field("Reason", punishment.getReason(), false);

        if (punishment.isPermanent()) {
            embed.field("Duration", "**Permanent**", true);
        } else if (punishment.getDuration() > 0) {
            embed.field("Duration", punishment.getDurationString(), true);
        }

        embed.field("Server", punishment.getServerOrigin(), true)
            .field("Scope", punishment.getServerScope(), true)
            .footer("CasualBans • " + TimeUtil.formatDate(punishment.getDateStart()))
            .timestamp(punishment.getDateStart());

        if (punishment.isSilent()) embed.title(title + " (Silent)");

        send(punishmentUrl, embed.build());
    }

    public void sendAltWebhook(String playerName, String ip, List<AccountInfo> accounts) {
        if (!enabled || altUrl.isEmpty()) return;

        StringBuilder desc = new StringBuilder();
        desc.append("**").append(playerName).append("** joined with **").append(accounts.size())
            .append("** known accounts on **").append(ip).append("**\n\n");

        for (AccountInfo a : accounts) {
            String icon = a.online() ? "🟢" : a.banned() ? "🔴" : "⚫";
            desc.append(icon).append(" `").append(a.name()).append("`");
            if (a.banned()) desc.append(" (BANNED)");
            if (a.muted()) desc.append(" (MUTED)");
            desc.append("\n");
        }

        Embed embed = new Embed()
            .title("👥 Alt Accounts Detected")
            .color(0xFEE75C)
            .description(desc.toString())
            .footer("CasualBans • IP: " + ip)
            .timestamp(System.currentTimeMillis());

        send(altUrl, embed.build());
    }

    public void sendStaffWebhook(String title, String description, int color) {
        if (!enabled || staffUrl.isEmpty()) return;
        Embed embed = new Embed()
            .title(title)
            .color(color)
            .description(description)
            .timestamp(System.currentTimeMillis());
        send(staffUrl, embed.build());
    }

    private void send(String url, String json) {
        plugin.getAsyncExecutor().execute(() -> {
            try {
                URL u = URI.create(url).toURL();
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "CasualBans/1.0.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code < 200 || code > 299) {
                    plugin.getLogger().warning("Discord webhook returned " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Discord webhook failed: " + e.getMessage());
            }
        });
    }

    // ── Simple embed builder ──

    private static class Embed {
        private final StringBuilder sb = new StringBuilder();
        private boolean hasTitle;

        Embed title(String t) { sb.append(",\"embeds\":[{\"title\":\"").append(escape(t)).append("\""); hasTitle = true; return this; }
        Embed color(int c) { sb.append(",\"color\":").append(c); return this; }
        Embed description(String d) { sb.append(",\"description\":\"").append(escape(d)).append("\""); return this; }
        Embed field(String n, String v, boolean inline) {
            sb.append(",\"fields\":[{\"name\":\"").append(escape(n))
                .append("\",\"value\":\"").append(escape(v))
                .append("\",\"inline\":").append(inline).append("}]");
            return this;
        }
        Embed footer(String f) { sb.append(",\"footer\":{\"text\":\"").append(escape(f)).append("\"}"); return this; }
        Embed timestamp(long t) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            sb.append(",\"timestamp\":\"").append(sdf.format(new java.util.Date(t))).append("\"");
            return this;
        }

        String build() {
            if (!hasTitle) sb.append(",\"embeds\":[{}]");
            sb.append("}]");
            return "{\"username\":\"CasualBans\"" + sb.substring(1) + "}";
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
        }
    }
}
