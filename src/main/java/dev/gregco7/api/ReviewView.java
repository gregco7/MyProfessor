package dev.gregco7.api;

import dev.gregco7.kc.Node;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * The answer key for a lesson, plus what the learner actually gave.
 *
 * <p>Served only after the lesson has been attempted. Before that it is a 409,
 * because handing over the key on request would make every lesson optional to
 * think about.
 *
 * <p>Written answers come back with their rubric and model answer but no mark.
 * Judging prose is the review pass that does not exist yet, and an unearned
 * number in its place would be worse than an honest blank.
 */
public record ReviewView(UUID nodeId, String subtopic, List<Q> questions) {

    /**
     * @param yourAnswer  what was submitted: the prose for a written question,
     *                    or comma-joined choice ids for multiple choice
     * @param correct     null for a written question, which is unmarked
     */
    public record Q(UUID questionId, short ordinal, QuestionType type, String body,
                    String yourAnswer, Boolean correct, String rubric, String modelAnswer,
                    List<C> choices) {}

    /** @param explanation why this option is right or wrong */
    public record C(UUID choiceId, String body, boolean isCorrect, String explanation) {}

    public static ReviewView of(Node node) {
        return new ReviewView(node.getNodeId(), node.getSubtopic(),
                node.getQuestions().stream().map(ReviewView::question).toList());
    }

    private static Q question(Question question) {
        boolean written = question.getType() == QuestionType.WRITTEN;
        return new Q(
                question.getQuestionId(),
                question.getOrdinal(),
                question.getType(),
                question.getBody(),
                written ? question.getAnswerWritten() : question.getAnswerMc(),
                written ? null : question.getGradeMc(),
                question.getRubric(),
                question.getModelAnswer(),
                question.getChoices().stream()
                        .map(choice -> new C(choice.getChoiceId(), choice.getBody(),
                                choice.isCorrect(), choice.getExplanation()))
                        .toList());
    }
}
