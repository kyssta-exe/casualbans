package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A warning action - automatically triggered when a player reaches X warnings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarnAction {
    private int threshold;
    private boolean thresholdOrMore; // If true, triggers at threshold OR MORE
    private String command;          // The action command to execute
    private boolean inheritSilent;   // Whether to inherit -s:$silent flag
}
