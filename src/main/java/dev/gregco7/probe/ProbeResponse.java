package dev.gregco7.probe;

import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * A probe as it is handed to the learner to sit.
 *
 * <p>Deliberately narrower than the rows behind it. Which choices are correct,
 * the model answer, the rubric and the two hints all exist on the question, and
 * none of them are sent: a diagnostic that ships its own answer key measures the
 * answer key rather than the learner.
 */
public record ProbeResponse(
        UUID probeId,
        String topic,
        short lvl,
        List<ProbeQuestion> questions) {

    /** @param multiSelect null for a written question; the learner needs it to know how to answer */
    public record ProbeQuestion(
            UUID questionId,
            short ordinal,
            QuestionType type,
            String body,
            Boolean multiSelect,
            List<ProbeChoice> choices) {}

    public record ProbeChoice(UUID choiceId, String body) {}

    public static ProbeResponse of(Probe probe) {
        return new ProbeResponse(
                probe.getProbeId(),
                probe.getTopic(),
                probe.getGoalProficiency(),
                probe.getQuestions().stream().map(ProbeResponse::question).toList());
    }

    private static ProbeQuestion question(Question question) {
        List<ProbeChoice> choices = question.getType() == QuestionType.MULTIPLE_CHOICE
                ? question.getChoices().stream()
                        .map(choice -> new ProbeChoice(choice.getChoiceId(), choice.getBody()))
                        .toList()
                : List.of();

        return new ProbeQuestion(
                question.getQuestionId(),
                question.getOrdinal(),
                question.getType(),
                question.getBody(),
                question.getMultiSelect(),
                choices);
    }
}
