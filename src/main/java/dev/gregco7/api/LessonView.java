package dev.gregco7.api;

import dev.gregco7.kc.Learn;
import dev.gregco7.kc.Node;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One lesson: the material to read, then the questions that test it.
 *
 * <p>Hints are included here, unlike on a diagnostic. A lesson is meant to be
 * worked through with help available; the client decides when to reveal them.
 * Which choices are correct, the rubric and the model answer are still withheld
 * — those arrive from {@code GET /api/nodes/{nodeId}/review}, and only once the
 * lesson has actually been attempted.
 */
public record LessonView(
        UUID nodeId,
        String subtopic,
        short passRequirement,
        boolean passed,
        Instant lastAttemptedTime,
        List<Section> sections,
        List<Q> questions) {

    /** @param format always MARKDOWN_LATEX: Markdown with $ ... $ and $$ ... $$ for maths */
    public record Section(UUID learnId, short ordinal, String body, String format) {}

    public record Q(UUID questionId, short ordinal, QuestionType type, String body,
                    String bodyFormat, Boolean multiSelect, String hint1, String hint2,
                    List<C> choices) {}

    public record C(UUID choiceId, String body) {}

    public static LessonView of(Node node) {
        return new LessonView(
                node.getNodeId(),
                node.getSubtopic(),
                node.getPassRequirement(),
                node.isPassed(),
                node.getLastAttemptedTime(),
                node.getLearnSections().stream().map(LessonView::section).toList(),
                node.getQuestions().stream().map(LessonView::question).toList());
    }

    private static Section section(Learn learn) {
        return new Section(learn.getLearnId(), learn.getOrdinal(), learn.getBody(),
                learn.getLearnFormat().name());
    }

    private static Q question(Question question) {
        List<C> choices = question.getType() == QuestionType.MULTIPLE_CHOICE
                ? question.getChoices().stream()
                        .map(choice -> new C(choice.getChoiceId(), choice.getBody()))
                        .toList()
                : List.of();
        return new Q(question.getQuestionId(), question.getOrdinal(), question.getType(),
                question.getBody(), question.getBodyFormat().name(), question.getMultiSelect(),
                question.getHint1(), question.getHint2(), choices);
    }
}
