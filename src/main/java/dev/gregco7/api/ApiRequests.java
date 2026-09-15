package dev.gregco7.api;

import dev.gregco7.kc.SubmittedAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Request bodies the dashboard posts. */
public final class ApiRequests {

    private ApiRequests() {}

    /**
     * @param lvl the proficiency being aimed at, 1 aware to 4 fluent; it sets how
     *            far up the diagnostic reaches, not where it starts
     */
    public record CreateProbe(@NotBlank String topic, @Min(1) @Max(4) short lvl) {}

    /**
     * @param numNodes how many lessons to plan; null lets lvl decide (3/5/7/9)
     * @param probeId  the diagnostic this learner sat. Null plans from the topic
     *                 alone, which necessarily starts at the beginning of it.
     * @param answers  what they gave for that diagnostic; questions left out
     *                 count as unanswered, which the planner reads as evidence
     */
    public record CreateSession(
            @NotBlank String topic,
            @Min(1) @Max(4) short lvl,
            @Min(2) @Max(12) Integer numNodes,
            UUID probeId,
            @Valid List<Answer> answers) {}

    /** @param answers one entry per question answered; omit a question to leave it blank */
    public record Attempt(@Valid @NotNull List<Answer> answers) {}

    /**
     * @param written   the prose, for a WRITTEN question
     * @param choiceIds the options picked, for a MULTIPLE_CHOICE question
     */
    public record Answer(@NotNull UUID questionId, String written, List<UUID> choiceIds) {

        SubmittedAnswer toSubmitted() {
            return new SubmittedAnswer(questionId, written, choiceIds);
        }
    }

    static List<SubmittedAnswer> toSubmitted(List<Answer> answers) {
        return answers == null ? List.of() : answers.stream().map(Answer::toSubmitted).toList();
    }
}
