package dev.gregco7.probe;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * What the learner gave for one probe question. Which field carries the answer
 * follows the question's own shape; the other is ignored.
 *
 * @param written   the learner's prose, for a written question
 * @param choiceIds the options they picked, for a multiple-choice question
 */
public record ProbeAnswer(
        @NotNull UUID questionId,
        String written,
        List<UUID> choiceIds) {}
