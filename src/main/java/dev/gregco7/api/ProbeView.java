package dev.gregco7.api;

import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;
import dev.gregco7.probe.Probe;

import java.util.List;
import java.util.UUID;

/**
 * A diagnostic as it is handed to the learner to sit.
 *
 * <p>Deliberately narrower than the rows behind it. Which choices are correct,
 * the rubric, the model answer and both hints all exist on the question and
 * none of them are sent — a diagnostic that ships its own answer key, or that
 * offers a nudge, measures the key or the nudge rather than the learner.
 */
public record ProbeView(UUID probeId, String topic, short lvl, List<Q> questions) {

    /** @param multiSelect null for a written question; the client needs it to know the input shape */
    public record Q(UUID questionId, short ordinal, QuestionType type, String body,
                    String bodyFormat, Boolean multiSelect, List<C> choices) {}

    public record C(UUID choiceId, String body) {}

    public static ProbeView of(Probe probe) {
        return new ProbeView(
                probe.getProbeId(), probe.getTopic(), probe.getGoalProficiency(),
                probe.getQuestions().stream().map(ProbeView::question).toList());
    }

    private static Q question(Question question) {
        List<C> choices = question.getType() == QuestionType.MULTIPLE_CHOICE
                ? question.getChoices().stream()
                        .map(choice -> new C(choice.getChoiceId(), choice.getBody()))
                        .toList()
                : List.of();
        return new Q(question.getQuestionId(), question.getOrdinal(), question.getType(),
                question.getBody(), question.getBodyFormat().name(),
                question.getMultiSelect(), choices);
    }
}
