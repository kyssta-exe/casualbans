package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a staff group with configured limits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffGroup {
    private String name;
    private String permission;
    private long maxTempBanDuration;   // -1 = unlimited
    private long maxTempMuteDuration;  // -1 = unlimited
    private int cooldownBan;           // seconds
    private int cooldownMute;
    private int cooldownWarn;
    private int cooldownKick;
    private int cooldownRedo;
    private boolean requireTemplate;
    private int weight;                // For hierarchical exemption (higher = more authority)
}
