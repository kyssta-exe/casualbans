package com.kyssta.casualbans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Component> COMPONENT_CACHE = new ConcurrentHashMap<>();

    // Main config messages
    private static final Map<String, String> MESSAGES = new HashMap<>();
    private static String PREFIX = "<gradient:#8c75a5:#f46c90>CasualBans</gradient> <dark_gray>»</dark_gray> ";

    private MessageUtil() {}

    public static void setPrefix(String prefix) {
        PREFIX = prefix;
    }

    public static void loadMessages(Map<String, Object> messages) {
        MESSAGES.clear();
        COMPONENT_CACHE.clear();
        CACHE.clear();
        if (messages != null) {
            messages.forEach((key, value) -> MESSAGES.put(key, String.valueOf(value)));
        }
    }

    public static String getMessage(String key, String... placeholders) {
        String msg = MESSAGES.getOrDefault(key, key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1] != null ? placeholders[i + 1] : "");
        }
        return msg;
    }

    public static Component format(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component prefix() {
        return format(PREFIX);
    }

    public static Component prefix(String message) {
        return format(PREFIX + message);
    }

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(format(message));
    }

    public static void send(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    public static void sendPrefix(CommandSender sender, String message) {
        sender.sendMessage(format(PREFIX + message));
    }

    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(format(PREFIX + "<red>" + message + "</red>"));
    }

    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(format(PREFIX + "<green>" + message + "</green>"));
    }

    public static void broadcast(String message, String permission) {
        String formatted = PREFIX + message;
        Component component = format(formatted);
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
        org.bukkit.Bukkit.getConsoleSender().sendMessage(component);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
            format(title),
            format(subtitle),
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
            )
        ));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(format(message));
    }

    public static String legacy(String minimessage) {
        return CACHE.computeIfAbsent(minimessage, k ->
            LEGACY.serialize(MINI_MESSAGE.deserialize(k))
        );
    }

    public static void shutdown() {
        CACHE.clear();
        COMPONENT_CACHE.clear();
        MESSAGES.clear();
    }
}
