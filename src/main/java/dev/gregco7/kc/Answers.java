package dev.gregco7.kc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes a submitted answer onto its question, and says whether it was right.
 *
 * <p>Shared by the diagnostic and by lesson attempts so that "answered
 * correctly" means one thing in this codebase rather than two.
 *
 * <p>Written answers are stored but not graded. Judging prose against a rubric
 * is a job for the review pass that does not exist yet, and guessing at it here
 * would put a number in grade_written that nothing had actually earned.
 */
public final class Answers {

    private Answers() {}

    /**
     * @return true when the question is now known to have been answered
     *         correctly; always false for written answers, which are unmarked
     */
    public static boolean record(Question question, SubmittedAnswer answer) {
        if (question.getType() == QuestionType.WRITTEN) {
            if (answer.written() == null || answer.written().isBlank()) {
                return false;
            }
            question.submitWrittenAnswer(answer.written(), null);
            return false;
        }
        return recordMultipleChoice(question, answer);
    }

    private static boolean recordMultipleChoice(Question question, SubmittedAnswer answer) {
        Set<UUID> valid = question.getChoices().stream()
                .map(Choice::getChoiceId)
                .collect(Collectors.toSet());

        Set<UUID> picked = new LinkedHashSet<>();
        for (UUID choiceId : answer.choiceIds() != null ? answer.choiceIds() : List.<UUID>of()) {
            if (valid.contains(choiceId)) {
                picked.add(choiceId);
            }
        }
        if (picked.isEmpty()) {
            return false;
        }

        Set<UUID> correct = question.getChoices().stream()
                .filter(Choice::isCorrect)
                .map(Choice::getChoiceId)
                .collect(Collectors.toSet());

        // Exact match, so picking every option cannot count as finding the right one.
        boolean right = picked.equals(correct);

        // kc.questions.answer_mc takes the selections concatenated into one column.
        question.submitMcAnswer(
                picked.stream().map(UUID::toString).collect(Collectors.joining(",")), right);
        return right;
    }
}
