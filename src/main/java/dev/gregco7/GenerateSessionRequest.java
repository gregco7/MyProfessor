package dev.gregco7;

import dev.gregco7.probe.ProbeAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * What the caller asks Claude to build a session out of.
 *
 * @param topic    what the learner wants to study
 * @param lvl      goal proficiency, 1 (aware) to 4 (fluent), matching app.sessions.lvl
 * @param numNodes how many lessons to break the topic into; null lets the goal
 *                 proficiency decide, and the response reports what was built
 * @param probeId  the diagnostic this learner sat, from POST /api/claude/probes.
 *                 Null plans from the topic alone, which means starting at the
 *                 beginning of it — pass one to have the session start instead at
 *                 the edge of what this learner already knows.
 * @param answers  what they gave for that diagnostic's questions; questions left
 *                 out count as unanswered, which the planner reads as evidence
 *                 rather than ignoring
 */
public record GenerateSessionRequest(
        @NotBlank String topic,
        @Min(1) @Max(4) short lvl,
        @Min(2) @Max(12) Integer numNodes,
        UUID probeId,
        @Valid List<ProbeAnswer> answers) {

    /** Higher goal proficiency means a finer breakdown of the same topic. */
    public int resolvedNodeCount() {
        return numNodes != null ? numNodes : switch (lvl) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            default -> 9;
        };
    }
}
