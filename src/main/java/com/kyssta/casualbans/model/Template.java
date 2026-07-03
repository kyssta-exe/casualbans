package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a punishment template with ladder progression.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    private String name;
    private PunishmentType type; // BAN, MUTE, WARN, KICK
    private String reason;
    private String message;
    private String broadcast;
    private long duration; // -1 = permanent
    private String permission;
    @Builder.Default
    private List<String> actions = new ArrayList<>();
    @Builder.Default
    private List<String> flags = new ArrayList<>();
    @Builder.Default
    private List<TemplateLadderStep> ladder = new ArrayList<>();
    private long expireLadder; // How long punishments stay on ladder
    private boolean ipTemplate; // Share progression across accounts on same IP

    public TemplateLadderStep getStep(int offenseCount) {
        if (ladder.isEmpty()) return null;
        // Ladder is ordered highest to lowest, find the matching step
        if (offenseCount >= ladder.size()) {
            return ladder.get(0); // Final step repeats
        }
        return ladder.get(offenseCount);
    }
}
