package dev.gregco7;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * What the caller asks Claude to build a diagnostic out of.
 *
 * @param topic what the learner wants to study
 * @param lvl   goal proficiency, 1 (aware) to 4 (fluent); it sets how far up the
 *              questions reach, not where they start
 */
public record GenerateProbeRequest(
        @NotBlank String topic,
        @Min(1) @Max(4) short lvl) {}
