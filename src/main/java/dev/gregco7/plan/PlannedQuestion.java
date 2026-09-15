package dev.gregco7.plan;

import dev.gregco7.kc.Choice;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;

import java.util.List;

/**
 * One question as Claude wrote it, before it is attached to a lesson or a probe.
 *
 * <p>The two question shapes share one record because Claude picks the shape per
 * question. The fields that do not apply to the chosen {@code type} come back
 * null and are dropped on the way to the row.
 */
public record PlannedQuestion(
        QuestionType type,
        String body,
        String hint1,
        String hint2,
        Boolean multiSelect,
        List<PlannedChoice> choices,
        String rubric,
        String modelAnswer) {

    /**
     * Builds the row for this question, or returns null if it is missing
     * something its shape requires. Callers drop the nulls rather than failing:
     * one unusable question out of ten is not worth throwing away the rest of a
     * lesson that took a minute of Claude's time to write.
     *
     * @param ordinal position within the lesson or probe this question belongs to
     */
    public Question toQuestion(short ordinal) {
        if (type == null || isBlank(body) || isBlank(hint1) || isBlank(hint2)) {
            return null;
        }

        if (type == QuestionType.WRITTEN) {
            return isBlank(rubric) ? null
                    : Question.written(ordinal, body, hint1, hint2, rubric, modelAnswer);
        }

        List<PlannedChoice> usable = (choices != null ? choices : List.<PlannedChoice>of()).stream()
                .filter(choice -> choice != null && !isBlank(choice.body()))
                .toList();
        long correct = usable.stream().filter(PlannedChoice::correct).count();
        if (usable.size() < 2 || correct == 0) {
            return null;
        }

        // Derived rather than taken from the response, so the flag can never
        // disagree with the choices actually attached to the question.
        Question question = Question.multipleChoice(ordinal, body, hint1, hint2, correct > 1);
        usable.forEach(choice ->
                question.addChoice(new Choice(choice.body(), choice.correct(), choice.explanation())));
        return question;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
