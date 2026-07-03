package com.kyssta.casualbans.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * A Template Group is a weighted multi-template ladder system.
 * Multiple templates (e.g. Spam, Advertising, Toxicity) share ONE ladder
 * with decimal weights. When the combined weight crosses an integer
 * threshold, the ladder escalates to the next step.
 * <p>
 * Example: ChatOffenses group with Spam(0.25) + Advertising(0.50) + Toxicity(0.20)
 *   - 1 Spam + 1 Advertising = 0.75 weight → step 0
 *   - 2 Spam + 1 Advertising = 1.00 weight → step 1
 *   - 4 Advertising            = 2.00 weight → step 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateGroup {

    private String name;
    private PunishmentType type;

    /** Template name → weight multiplier for this group. */
    @Builder.Default
    private Map<String, Double> weights = new LinkedHashMap<>();

    /** Escalation ladder steps ordered from lowest threshold upward. */
    @Builder.Default
    private List<TemplateLadderStep> ladderSteps = new ArrayList<>();

    /** How long before a punishment drops off the ladder (millis). */
    private long expireLadder;

    /**
     * Get the appropriate ladder step based on the combined weight.
     * Steps are indexed by the integer floor of the combined weight.
     *
     * @param combinedWeight the summed (count × weight) across all templates
     * @return the matching ladder step, or the final step if weight exceeds the ladder
     */
    public TemplateLadderStep getStepByWeight(double combinedWeight) {
        if (ladderSteps.isEmpty()) return null;

        int index = (int) Math.floor(combinedWeight);
        if (index < 0) index = 0;
        if (index >= ladderSteps.size()) {
            return ladderSteps.get(ladderSteps.size() - 1);
        }
        return ladderSteps.get(index);
    }
}
