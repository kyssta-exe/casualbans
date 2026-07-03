package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A single step in a template's ladder.
 * Ladder steps are ordered from highest (final) to lowest (first offense).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateLadderStep {
    private String reason;
    private String message;
    private String broadcast;
    private long duration; // -1 = inherit from template, 0 = inherit, specific = override
    @Builder.Default
    private List<String> actions = new ArrayList<>();
    @Builder.Default
    private List<String> flags = new ArrayList<>();
}
