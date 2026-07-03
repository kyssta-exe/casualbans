package com.kyssta.casualbans.util;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Punishment;
import com.kyssta.casualbans.model.PunishmentType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Built-in web server for CasualBans.
 * Serves a lightweight dark-themed web interface for bans/mutes/history.
 */
public class WebServer {

    private static final String CSS = ""
        + "* { margin: 0; padding: 0; box-sizing: border-box; }\n"
        + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n"
        + "  background: #0d1117; color: #c9d1d9; min-height: 100vh; }\n"
        + ".container { max-width: 1000px; margin: 0 auto; padding: 2rem; }\n"
        + "header { text-align: center; margin-bottom: 2rem; padding: 2rem; }\n"
        + "h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }\n"
        + ".accent { color: #8c75a5; }\n"
        + ".subtitle { color: #8b949e; }\n"
        + ".stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 2rem; }\n"
        + ".stat-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 1.5rem; text-align: center; }\n"
        + ".stat-card h3 { font-size: 2rem; color: #f46c90; }\n"
        + ".stat-card p { color: #8b949e; margin-top: 0.5rem; }\n"
        + "nav { display: flex; gap: 1rem; justify-content: center; margin-bottom: 2rem; flex-wrap: wrap; }\n"
        + ".btn { display: inline-block; padding: 0.75rem 1.5rem; background: #21262d; color: #c9d1d9; text-decoration: none;\n"
        + "  border: 1px solid #30363d; border-radius: 6px; transition: all 0.2s; }\n"
        + ".btn:hover { background: #30363d; border-color: #8c75a5; }\n"
        + "table { width: 100%; border-collapse: collapse; margin-top: 1rem; }\n"
        + "th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #21262d; }\n"
        + "th { color: #8b949e; font-weight: 600; text-transform: uppercase; font-size: 0.8rem; }\n"
        + "td { color: #c9d1d9; }\n"
        + "tr:hover td { background: #161b22; }\n"
        + ".active { color: #f46c90; }\n"
        + ".expired { color: #8b949e; }\n"
        + ".search-box { display: flex; gap: 0.5rem; margin: 2rem 0; justify-content: center; }\n"
        + ".search-box input { padding: 0.75rem 1rem; background: #0d1117; border: 1px solid #30363d;\n"
        + "  border-radius: 6px; color: #c9d1d9; width: 300px; font-size: 1rem; }\n"
        + ".search-box button { padding: 0.75rem 1.5rem; background: #8c75a5; color: #fff;\n"
        + "  border: none; border-radius: 6px; cursor: pointer; font-size: 1rem; }\n"
        + ".search-box button:hover { background: #7a6390; }\n"
        + "pre { background: #161b22; padding: 1rem; border-radius: 8px; overflow-x: auto; }\n"
        + ".error { color: #f85149; text-align: center; padding: 2rem; }\n";

    private final CasualBans plugin;
    private final Gson gson;
    private HttpServer server;
    private final int port;

    public WebServer(CasualBans plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.port = plugin.getConfig().getInt("web.port", 8291);
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(2));

            server.createContext("/", this::handleIndex);
            server.createContext("/bans", this::handleBans);
            server.createContext("/mutes", this::handleMutes);
            server.createContext("/warnings", this::handleWarnings);
            server.createContext("/kicks", this::handleKicks);
            server.createContext("/history", this::handleHistory);
            server.createContext("/api/bans", this::handleApiBans);
            server.createContext("/api/mutes", this::handleApiMutes);
            server.createContext("/api/history", this::handleApiHistory);
            server.createContext("/api/stats", this::handleApiStats);
            server.createContext("/api/check", this::handleApiCheck);
            server.createContext("/style.css", this::handleCss);

            server.start();
            plugin.getLogger().info("Web interface started on http://0.0.0.0:" + port);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not start web interface: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        String html = buildPage("CasualBans", ""
            + "<div class=\"stats\" id=\"stats\">"
            + "  <div class=\"stat-card\"><h3>Loading...</h3></div>"
            + "</div>"
            + "<nav>"
            + "  <a href=\"/bans\" class=\"btn\">Active Bans</a>"
            + "  <a href=\"/mutes\" class=\"btn\">Active Mutes</a>"
            + "  <a href=\"/warnings\" class=\"btn\">Active Warnings</a>"
            + "  <a href=\"/history\" class=\"btn\">Punishment History</a>"
            + "</nav>"
            + "<div class=\"search-box\">"
            + "  <input type=\"text\" id=\"playerSearch\" placeholder=\"Search player...\">"
            + "  <button onclick=\"searchPlayer()\">Search</button>"
            + "</div>"
            + "<div id=\"searchResult\"></div>"
            + "<script>"
            + "async function loadStats() {"
            + "  const r = await fetch('/api/stats'); const d = await r.json();"
            + "  document.getElementById('stats').innerHTML = ''"
            + "    + '<div class=\"stat-card\"><h3>'+d.activeBans+'</h3><p>Active Bans</p></div>'"
            + "    + '<div class=\"stat-card\"><h3>'+d.activeMutes+'</h3><p>Active Mutes</p></div>'"
            + "    + '<div class=\"stat-card\"><h3>'+d.activeWarnings+'</h3><p>Active Warnings</p></div>'"
            + "    + '<div class=\"stat-card\"><h3>'+d.totalPunishments+'</h3><p>Total Punishments</p></div>';"
            + "}"
            + "async function searchPlayer() {"
            + "  const name = document.getElementById('playerSearch').value;"
            + "  const r = await fetch('/api/check?player='+encodeURIComponent(name));"
            + "  const d = await r.json();"
            + "  const div = document.getElementById('searchResult');"
            + "  if(d.error) div.innerHTML = '<div class=\"error\">'+d.error+'</div>';"
            + "  else div.innerHTML = '<pre>'+JSON.stringify(d,null,2)+'</pre>';"
            + "}"
            + "loadStats();"
            + "</script>"
        );
        sendResponse(exchange, 200, "text/html", html);
    }

    private void handleBans(HttpExchange exchange) throws IOException {
        sendPunishmentList(exchange, "Active Bans",
            PunishmentType.BAN, PunishmentType.TEMPBAN, PunishmentType.IPBAN);
    }

    private void handleMutes(HttpExchange exchange) throws IOException {
        sendPunishmentList(exchange, "Active Mutes",
            PunishmentType.MUTE, PunishmentType.TEMPMUTE, PunishmentType.IPMUTE);
    }

    private void handleWarnings(HttpExchange exchange) throws IOException {
        sendPunishmentList(exchange, "Active Warnings", PunishmentType.WARN);
    }

    private void handleKicks(HttpExchange exchange) throws IOException {
        sendPunishmentList(exchange, "Recent Kicks", PunishmentType.KICK);
    }

    private void sendPunishmentList(HttpExchange exchange, String title, PunishmentType... types) throws IOException {
        List<Punishment> all = new ArrayList<>();
        for (PunishmentType t : types) {
            List<Punishment> list = plugin.getStorageProvider().getAllActivePunishments(t, "*");
            if (list != null) all.addAll(list);
        }

        StringBuilder rows = new StringBuilder();
        for (Punishment p : all) {
            rows.append("<tr>")
                .append("<td>").append(esc(p.getName())).append("</td>")
                .append("<td>").append(esc(p.getReason())).append("</td>")
                .append("<td>").append(esc(p.getExecutorName())).append("</td>")
                .append("<td>").append(TimeUtil.formatDate(p.getDateStart())).append("</td>")
                .append("<td>").append(esc(p.getDurationString())).append("</td>")
                .append("</tr>");
        }

        String content = "<a href=\"/\" class=\"btn\">&laquo; Back</a>\n"
            + "<table>\n<tr><th>Player</th><th>Reason</th><th>By</th><th>Date</th><th>Expires</th></tr>\n"
            + (rows.isEmpty()
                ? "<tr><td colspan='5' style='text-align:center;padding:2rem;color:#8b949e'>No entries</td></tr>"
                : rows.toString())
            + "</table>";

        sendResponse(exchange, 200, "text/html", buildPage(title + " (" + all.size() + ")", content));
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String player = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("player"))
                    player = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }

        StringBuilder content = new StringBuilder();
        content.append("<a href=\"/\" class=\"btn\">&laquo; Back</a>\n");
        content.append("<div class=\"search-box\">\n");
        content.append("  <form method=\"get\">\n");
        content.append("    <input type=\"text\" name=\"player\" placeholder=\"Player name\" value=\"")
            .append(player != null ? esc(player) : "").append("\">\n");
        content.append("    <button type=\"submit\">Search</button>\n");
        content.append("  </form>\n");
        content.append("</div>\n");

        if (player != null) {
            var uuid = UUIDUtil.resolveUUID(player);
            if (uuid != null) {
                var history = plugin.getStorageProvider().getHistory(uuid, 50);
                content.append("<table><tr><th>Type</th><th>Reason</th><th>By</th><th>Date</th><th>Status</th></tr>\n");
                for (Punishment p : history) {
                    String status = p.isActive() ? "<span class='active'>Active</span>"
                        : "<span class='expired'>Expired</span>";
                    content.append("<tr>")
                        .append("<td>").append(p.getType().getPastTense()).append("</td>")
                        .append("<td>").append(esc(p.getReason())).append("</td>")
                        .append("<td>").append(esc(p.getExecutorName())).append("</td>")
                        .append("<td>").append(TimeUtil.formatDate(p.getDateStart())).append("</td>")
                        .append("<td>").append(status).append("</td>")
                        .append("</tr>\n");
                }
                content.append("</table>");
            } else {
                content.append("<div class=\"error\">Player not found</div>");
            }
        }

        sendResponse(exchange, 200, "text/html", buildPage("Punishment History", content.toString()));
    }

    private void handleCss(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "text/css", CSS);
    }

    // API
    private void handleApiBans(HttpExchange exchange) throws IOException {
        List<Punishment> all = new ArrayList<>();
        addAll(all, PunishmentType.BAN);
        addAll(all, PunishmentType.TEMPBAN);
        addAll(all, PunishmentType.IPBAN);
        sendJson(exchange, all);
    }

    private void handleApiMutes(HttpExchange exchange) throws IOException {
        List<Punishment> all = new ArrayList<>();
        addAll(all, PunishmentType.MUTE);
        addAll(all, PunishmentType.TEMPMUTE);
        addAll(all, PunishmentType.IPMUTE);
        sendJson(exchange, all);
    }

    private void handleApiHistory(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.startsWith("player=")) {
            String player = URLDecoder.decode(query.substring(7), StandardCharsets.UTF_8);
            var uuid = UUIDUtil.resolveUUID(player);
            if (uuid != null) {
                sendJson(exchange, plugin.getStorageProvider().getHistory(uuid, 50));
                return;
            }
        }
        sendJson(exchange, Collections.singletonMap("error", "Player parameter required"));
    }

    private void handleApiStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeBans", count(PunishmentType.BAN) + count(PunishmentType.TEMPBAN) + count(PunishmentType.IPBAN));
        stats.put("activeMutes", count(PunishmentType.MUTE) + count(PunishmentType.TEMPMUTE) + count(PunishmentType.IPMUTE));
        stats.put("activeWarnings", count(PunishmentType.WARN));
        stats.put("totalPunishments", countAll());
        sendJson(exchange, stats);
    }

    private void handleApiCheck(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.startsWith("player=")) {
            String player = URLDecoder.decode(query.substring(7), StandardCharsets.UTF_8);
            var uuid = UUIDUtil.resolveUUID(player);
            if (uuid != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("player", player);
                result.put("banned", plugin.getPunishmentManager().isBanned(uuid, "*"));
                result.put("muted", plugin.getPunishmentManager().isMuted(uuid, "*"));
                var ban = plugin.getPunishmentManager().getActiveBan(uuid, "*");
                if (ban != null) result.put("ban", Map.of("reason", ban.getReason(),
                    "by", ban.getExecutorName(), "date", TimeUtil.formatDate(ban.getDateStart()),
                    "expires", ban.getDurationString()));
                var mute = plugin.getPunishmentManager().getActiveMute(uuid, "*");
                if (mute != null) result.put("mute", Map.of("reason", mute.getReason(),
                    "by", mute.getExecutorName(), "date", TimeUtil.formatDate(mute.getDateStart()),
                    "expires", mute.getDurationString()));
                sendJson(exchange, result);
                return;
            }
        }
        sendJson(exchange, Collections.singletonMap("error", "Player not found"));
    }

    private void sendResponse(HttpExchange exchange, int code, String contentType, String content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void sendJson(HttpExchange exchange, Object data) throws IOException {
        sendResponse(exchange, 200, "application/json", gson.toJson(data));
    }

    private String buildPage(String title, String content) {
        return "<!DOCTYPE html>\n<html>\n<head>"
            + "<meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>CasualBans - " + title + "</title>"
            + "<link rel=\"stylesheet\" href=\"/style.css\">"
            + "</head><body>"
            + "<div class=\"container\">"
            + "<header><h1><span class=\"accent\">Casual</span>Bans</h1>"
            + "<p class=\"subtitle\">" + title + "</p></header>"
            + content
            + "</div></body></html>";
    }

    private void addAll(List<Punishment> list, PunishmentType type) {
        List<Punishment> items = plugin.getStorageProvider().getAllActivePunishments(type, "*");
        if (items != null) list.addAll(items);
    }

    private int count(PunishmentType type) {
        return plugin.getStorageProvider().getActivePunishmentCount(type);
    }

    private int countAll() {
        return plugin.getStorageProvider().getTotalPunishments(PunishmentType.BAN)
            + plugin.getStorageProvider().getTotalPunishments(PunishmentType.MUTE)
            + plugin.getStorageProvider().getTotalPunishments(PunishmentType.WARN)
            + plugin.getStorageProvider().getTotalPunishments(PunishmentType.KICK);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
