package com.kyssta.casualbans;

import com.kyssta.casualbans.manager.*;
import com.kyssta.casualbans.storage.StorageProvider;
import com.kyssta.casualbans.storage.JsonStorage;
import com.kyssta.casualbans.storage.SqlStorage;
import com.kyssta.casualbans.util.MessageUtil;
import com.kyssta.casualbans.util.Validator;
import com.kyssta.casualbans.util.WebServer;
import com.kyssta.casualbans.util.WebhookManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Getter
public final class CasualBans extends JavaPlugin {

    @Getter
    private static CasualBans instance;

    // Managers
    private ConfigManager configManager;
    private PunishmentManager punishmentManager;
    private TemplateManager templateManager;
    private InvestigationManager investigationManager;
    private GeoIPManager geoIPManager;
    private ChatManager chatManager;
    private LockdownManager lockdownManager;
    private StaffManager staffManager;
    private SyncManager syncManager;

    // Storage
    private StorageProvider storageProvider;

    // Web interface
    private WebServer webServer;

    // Webhooks
    private WebhookManager webhookManager;

    // Thread pools
    private ScheduledExecutorService asyncExecutor;
    private ThreadPoolExecutor ioExecutor;

    // Server identity
    private String serverName;
    private String defaultServerScope;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        getLogger().info("Loading CasualBans v" + getDescription().getVersion() + "...");

        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("templates.yml", false);

        // Load config
        this.configManager = new ConfigManager(this);
        this.serverName = getConfig().getString("server.name", "casualbans");
        this.defaultServerScope = getConfig().getString("server.default-scope", "*");

        // Validate configuration
        Validator.validateConfig();

        // Load prefix & messages
        String prefix = getConfig().getString("display.prefix", "<gradient:#8c75a5:#f46c90>CasualBans</gradient> <dark_gray>»</dark_gray>");
        MessageUtil.setPrefix(prefix);

        // Init thread pools
        int asyncThreads = getConfig().getInt("performance.async-threads", 4);
        this.asyncExecutor = Executors.newScheduledThreadPool(
            asyncThreads,
            r -> new Thread(r, "CasualBans-Async-%d")
        );
        this.ioExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool(
            r -> new Thread(r, "CasualBans-IO-%d")
        );

        // Init storage
        initStorage();

        // Init managers
        this.punishmentManager = new PunishmentManager(this);
        this.templateManager = new TemplateManager(this);
        this.investigationManager = new InvestigationManager(this);
        this.geoIPManager = new GeoIPManager(this);
        this.chatManager = new ChatManager(this);
        this.lockdownManager = new LockdownManager(this);
        this.staffManager = new StaffManager(this);
        this.syncManager = new SyncManager(this);
        this.webhookManager = new WebhookManager(this);

        // Register commands
        CommandRegistry.registerCommands(this);

        // Register listeners
        ListenerRegistry.registerListeners(this);

        // Load manager data
        this.templateManager.loadTemplates();
        this.lockdownManager.load();

        // Start web interface
        if (getConfig().getBoolean("web.enabled", false)) {
            this.webServer = new WebServer(this);
            this.webServer.start();
        }

        // ── Startup status summary ──
        long elapsed = System.currentTimeMillis() - start;

        StringBuilder summary = new StringBuilder();
        summary.append("┌────────────────────────────────────────────────────────────┐\n");
        summary.append("│ CasualBans v").append(String.format("%-47s", getDescription().getVersion())).append("│\n");
        summary.append("├────────────────────────────────────────────────────────────┤\n");

        // Storage
        String driver = getConfig().getString("storage.driver", "JSON");
        String storageStatus = storageProvider != null && storageProvider.isConnected()
            ? "✔" : "✘";
        summary.append("│ Storage: ").append(String.format("%-52s", driver + "  " + storageStatus)).append("│\n");

        // GeoIP
        boolean geoipEnabled = configManager.isGeoipEnabled();
        String geoipStatus = geoipEnabled && geoIPManager != null && geoIPManager.isAvailable()
            ? "✔" : "disabled";
        summary.append("│ GeoIP:   ").append(String.format("%-52s",
            geoipEnabled ? "enabled  " + geoipStatus : "disabled")).append("│\n");

        // Sync
        boolean syncOn = syncManager != null && syncManager.isEnabled();
        summary.append("│ Sync:    ").append(String.format("%-52s",
            syncOn ? "enabled" : "disabled")).append("│\n");

        // Webhooks
        boolean webhookOn = webhookManager != null && webhookManager.isEnabled();
        summary.append("│ Webhooks:").append(String.format("%-52s",
            webhookOn ? "enabled" : "disabled")).append("│\n");

        // Lockdown
        boolean lockedDown = lockdownManager != null && lockdownManager.isLockedDown();
        summary.append("│ Lockdown:").append(String.format("%-52s",
            lockedDown ? "ACTIVE" : "inactive")).append("│\n");

        summary.append("│ Web:     ").append(String.format("%-52s",
            getConfig().getBoolean("web.enabled", false) ? "enabled" : "disabled")).append("│\n");

        summary.append("├────────────────────────────────────────────────────────────┤\n");
        summary.append("│ Started in ").append(String.format("%-49s", elapsed + "ms")).append("│\n");
        summary.append("└────────────────────────────────────────────────────────────┘");

        getLogger().info("\n" + summary.toString());
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        if (storageProvider != null) {
            storageProvider.shutdown();
        }
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                asyncExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
            }
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        MessageUtil.shutdown();
    }

    private void initStorage() {
        String driver = getConfig().getString("storage.driver", "JSON").toUpperCase();

        switch (driver) {
            case "MYSQL":
            case "MARIADB":
            case "POSTGRESQL":
            case "H2":
                this.storageProvider = new SqlStorage(this);
                break;
            case "JSON":
            default:
                this.storageProvider = new JsonStorage(this);
                break;
        }

        this.storageProvider.initialize();
        getLogger().info("Storage initialized: " + driver);
    }

    public File getDataFile(String name) {
        return new File(getDataFolder(), name);
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        templateManager.loadTemplates();
        if (storageProvider instanceof SqlStorage) {
            ((SqlStorage) storageProvider).reconnect();
        }
        getLogger().info("CasualBans reloaded.");
    }
}
