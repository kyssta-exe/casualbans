package com.kyssta.casualbans.manager;

import com.kyssta.casualbans.CasualBans;
import com.kyssta.casualbans.model.Template;
import com.kyssta.casualbans.model.TemplateGroup;
import com.kyssta.casualbans.model.TemplateLadderStep;
import com.kyssta.casualbans.model.PunishmentType;
import com.kyssta.casualbans.model.Punishment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages punishment templates with ladder progression.
 */
public class TemplateManager {

    private final CasualBans plugin;
    private final Map<String, Template> banTemplates = new ConcurrentHashMap<>();
    private final Map<String, Template> muteTemplates = new ConcurrentHashMap<>();
    private final Map<String, Template> warnTemplates = new ConcurrentHashMap<>();
    private final Map<String, Template> kickTemplates = new ConcurrentHashMap<>();
    private final Map<String, TemplateGroup> templateGroups = new ConcurrentHashMap<>();
    private FileConfiguration templatesConfig;

    public TemplateManager(CasualBans plugin) {
        this.plugin = plugin;
    }

    public void loadTemplates() {
        banTemplates.clear();
        muteTemplates.clear();
        warnTemplates.clear();
        kickTemplates.clear();
        templateGroups.clear();

        File file = new File(plugin.getDataFolder(), "templates.yml");
        if (!file.exists()) {
            plugin.saveResource("templates.yml", false);
        }
        templatesConfig = YamlConfiguration.loadConfiguration(file);

        loadTemplatesFromConfig("ban-templates", banTemplates, PunishmentType.BAN);
        loadTemplatesFromConfig("mute-templates", muteTemplates, PunishmentType.MUTE);
        loadTemplatesFromConfig("warn-templates", warnTemplates, PunishmentType.WARN);
        loadTemplatesFromConfig("kick-templates", kickTemplates, PunishmentType.KICK);
        loadTemplateGroups();

        plugin.getLogger().info("Loaded " + (banTemplates.size() + muteTemplates.size()
            + warnTemplates.size() + kickTemplates.size()) + " templates and "
            + templateGroups.size() + " template groups.");
    }

    private void loadTemplatesFromConfig(String path, Map<String, Template> map, PunishmentType type) {
        var section = templatesConfig.getConfigurationSection(path);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                var tplSection = section.getConfigurationSection(key);
                if (tplSection == null) continue;

                Template.TemplateBuilder builder = Template.builder()
                    .name(key)
                    .type(type)
                    .reason(tplSection.getString("reason"))
                    .message(tplSection.getString("message"))
                    .broadcast(tplSection.getString("broadcast"))
                    .permission(tplSection.getString("permission"))
                    .actions(tplSection.getStringList("actions"))
                    .flags(tplSection.getStringList("flags"))
                    .ipTemplate(tplSection.getBoolean("ip_template", false));

                String durationStr = tplSection.getString("duration", "permanent");
                long duration = Punishment.parseDuration(durationStr);
                builder.duration(duration);

                String expireStr = tplSection.getString("expire_ladder", "90d");
                long expireDuration = Punishment.parseDuration(expireStr);
                builder.expireLadder(expireDuration);

                // Load ladder
                var ladderSection = tplSection.getConfigurationSection("ladder");
                if (ladderSection != null) {
                    List<com.kyssta.casualbans.model.TemplateLadderStep> steps = new ArrayList<>();
                    for (String stepKey : ladderSection.getKeys(false)) {
                        var stepSection = ladderSection.getConfigurationSection(stepKey);
                        if (stepSection == null) continue;

                        var stepBuilder = com.kyssta.casualbans.model.TemplateLadderStep.builder();
                        if (stepSection.contains("reason"))
                            stepBuilder.reason(stepSection.getString("reason"));
                        if (stepSection.contains("message"))
                            stepBuilder.message(stepSection.getString("message"));
                        if (stepSection.contains("duration")) {
                            stepBuilder.duration(Punishment.parseDuration(stepSection.getString("duration")));
                        }
                        if (stepSection.contains("actions"))
                            stepBuilder.actions(stepSection.getStringList("actions"));
                        if (stepSection.contains("flags"))
                            stepBuilder.flags(stepSection.getStringList("flags"));

                        steps.add(stepBuilder.build());
                    }
                    builder.ladder(steps);
                }

                map.put(key.toLowerCase(), builder.build());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load template '" + key + "': " + e.getMessage());
            }
        }
    }

    /**
     * Load template groups from the template-groups section of templates.yml.
     * Each group defines a set of templates with decimal weights that contribute
     * to a shared escalation ladder.
     */
    private void loadTemplateGroups() {
        var section = templatesConfig.getConfigurationSection("template-groups");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                var grpSection = section.getConfigurationSection(key);
                if (grpSection == null) continue;

                TemplateGroup.TemplateGroupBuilder builder = TemplateGroup.builder()
                    .name(key)
                    .type(PunishmentType.valueOf(grpSection.getString("type", "MUTE").toUpperCase()));

                // Load weights (template name → weight)
                var weightsSection = grpSection.getConfigurationSection("weights");
                if (weightsSection != null) {
                    Map<String, Double> weights = new LinkedHashMap<>();
                    for (String tplName : weightsSection.getKeys(false)) {
                        weights.put(tplName, weightsSection.getDouble(tplName, 0.0));
                    }
                    builder.weights(weights);
                }

                // Load ladder steps
                var ladderSection = grpSection.getConfigurationSection("ladder");
                if (ladderSection != null) {
                    List<TemplateLadderStep> steps = new ArrayList<>();
                    for (String stepKey : ladderSection.getKeys(false)) {
                        var stepSection = ladderSection.getConfigurationSection(stepKey);
                        if (stepSection == null) continue;

                        var stepBuilder = TemplateLadderStep.builder();
                        if (stepSection.contains("reason"))
                            stepBuilder.reason(stepSection.getString("reason"));
                        if (stepSection.contains("message"))
                            stepBuilder.message(stepSection.getString("message"));
                        if (stepSection.contains("broadcast"))
                            stepBuilder.broadcast(stepSection.getString("broadcast"));
                        if (stepSection.contains("duration")) {
                            stepBuilder.duration(Punishment.parseDuration(stepSection.getString("duration")));
                        }
                        if (stepSection.contains("actions"))
                            stepBuilder.actions(stepSection.getStringList("actions"));
                        if (stepSection.contains("flags"))
                            stepBuilder.flags(stepSection.getStringList("flags"));

                        steps.add(stepBuilder.build());
                    }
                    builder.ladderSteps(steps);
                }

                String expireStr = grpSection.getString("expire_ladder", "90d");
                builder.expireLadder(Punishment.parseDuration(expireStr));

                templateGroups.put(key.toLowerCase(), builder.build());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load template group '" + key + "': " + e.getMessage());
            }
        }
    }

    public Template getTemplate(String name, PunishmentType type) {
        if (name == null) return null;
        return switch (type.getBaseType()) {
            case BAN -> banTemplates.get(name.toLowerCase());
            case MUTE -> muteTemplates.get(name.toLowerCase());
            case WARN -> warnTemplates.get(name.toLowerCase());
            case KICK -> kickTemplates.get(name.toLowerCase());
            default -> null;
        };
    }

    /**
     * Get the number of times a player has been punished under a given template.
     */
    public int getOffenseCount(UUID uuid, String templateName) {
        return plugin.getStorageProvider().getOffenseCount(uuid, templateName, "*");
    }

    /**
     * Get the number of times an IP has been punished under a given template.
     */
    public int getOffenseCountByIP(String ip, String templateName) {
        return plugin.getStorageProvider().getOffenseCountByIP(ip, templateName);
    }

    /**
     * Get a loaded template group by name.
     *
     * @param name the group name (case-insensitive)
     * @return the template group, or null if not found
     */
    public TemplateGroup getTemplateGroup(String name) {
        if (name == null) return null;
        return templateGroups.get(name.toLowerCase());
    }

    /**
     * Get all loaded template groups.
     */
    public Collection<TemplateGroup> getAllTemplateGroups() {
        return templateGroups.values();
    }

    /**
     * Calculate the combined offense weight for a player within a template group.
     * Iterates the group's templates and sums (offense count × weight).
     *
     * @param uuid  the player's UUID
     * @param group the template group
     * @return the combined weight as a double
     */
    public double getCombinedOffenseWeight(UUID uuid, TemplateGroup group) {
        if (group == null || group.getWeights().isEmpty()) return 0.0;

        double total = 0.0;
        for (Map.Entry<String, Double> entry : group.getWeights().entrySet()) {
            String tplName = entry.getKey();
            double weight = entry.getValue();
            if (weight <= 0) continue;
            int count = getOffenseCount(uuid, tplName);
            total += count * weight;
        }
        return total;
    }

    /**
     * Calculate the combined offense weight for an IP within a template group.
     *
     * @param ip    the IP address
     * @param group the template group
     * @return the combined weight as a double
     */
    public double getCombinedOffenseWeightByIP(String ip, TemplateGroup group) {
        if (group == null || group.getWeights().isEmpty()) return 0.0;

        double total = 0.0;
        for (Map.Entry<String, Double> entry : group.getWeights().entrySet()) {
            String tplName = entry.getKey();
            double weight = entry.getValue();
            if (weight <= 0) continue;
            int count = getOffenseCountByIP(ip, tplName);
            total += count * weight;
        }
        return total;
    }

    /**
     * Get the appropriate ladder step for a player based on their offense count
     * or template group weight.
     * <p>
     * When {@code type} is {@code null}, the method will attempt to resolve
     * {@code templateName} as a template group name first, then fall back to
     * a regular template lookup.
     *
     * @param uuid         the player's UUID
     * @param templateName template name or template group name
     * @param type         punishment type (may be null for template group lookup)
     * @param ip           the player's IP (or null)
     * @return the matching ladder step, or null if none found
     */
    public TemplateLadderStep getNextStep(UUID uuid, String templateName,
                                          PunishmentType type, String ip) {
        // First, check if this is a template group
        TemplateGroup group = getTemplateGroup(templateName);
        if (group != null) {
            double weight;
            if (ip != null) {
                weight = getCombinedOffenseWeightByIP(ip, group);
            } else {
                weight = getCombinedOffenseWeight(uuid, group);
            }
            return group.getStepByWeight(weight);
        }

        // Fall back to regular template lookup
        if (type == null) return null;
        Template template = getTemplate(templateName, type);
        if (template == null) return null;
        if (template.getLadder() == null || template.getLadder().isEmpty()) return null;

        int offenseCount;
        if (template.isIpTemplate() && ip != null) {
            offenseCount = plugin.getStorageProvider().getOffenseCountByIP(ip, templateName);
        } else {
            offenseCount = getOffenseCount(uuid, templateName);
        }

        return template.getStep(offenseCount);
    }

    public void reload() {
        loadTemplates();
    }

    public Collection<Template> getAllTemplates(PunishmentType type) {
        return switch (type.getBaseType()) {
            case BAN -> banTemplates.values();
            case MUTE -> muteTemplates.values();
            case WARN -> warnTemplates.values();
            case KICK -> kickTemplates.values();
            default -> Collections.emptyList();
        };
    }
}
