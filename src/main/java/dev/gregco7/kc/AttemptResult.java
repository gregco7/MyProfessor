package dev.gregco7.kc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The outcome of one sitting of a lesson's questions.
 *
 * @param correct           questions marked right; written answers are never
 *                          among them until there is a review pass to mark them
 * @param total             questions in the lesson, answered or not
 * @param passRequirement   how many of them had to be right
 * @param passed            the lesson's standing now, which is sticky: a lesson
 *                          cleared earlier stays cleared even if this sitting
 *                          fell short
 * @param lastAttemptedTime when this sitting was recorded
 */
public record AttemptResult(
        UUID nodeId,
        int correct,
        int total,
        short passRequirement,
        boolean passed,
        Instant lastAttemptedTime,
        List<QuestionOutcome> outcomes) {

    /**
     * @param graded  false for a written answer, which is stored but unmarked,
     *                and false for one left blank
     * @param correct meaningful only where graded is true
     */
    public record QuestionOutcome(
            UUID questionId,
            short ordinal,
            QuestionType type,
            boolean answered,
            boolean graded,
            boolean correct) {}
}
