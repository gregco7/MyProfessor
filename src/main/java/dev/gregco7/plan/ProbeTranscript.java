package dev.gregco7.plan;

import dev.gregco7.kc.Choice;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;
import dev.gregco7.probe.Probe;

import java.util.Set;

/**
 * Renders a sat probe as the evidence Claude reads when planning the session:
 * each question, what a right answer was, and what the learner actually gave.
 *
 * <p>The correct answers are included deliberately. The planner is not marking
 * the paper — it is reading the shape of the mistakes, and a wrong choice only
 * says something about the learner next to the right one and to the explanation
 * of why someone would pick it instead.
 *
 * <p>An unanswered question is reported as unanswered rather than dropped. A
 * learner leaving the last three blank is one of the clearest signals in the
 * transcript.
 */
final class ProbeTranscript {

    private ProbeTranscript() {}

    static String render(Probe probe) {
        StringBuilder out = new StringBuilder();
        for (Question question : probe.getQuestions()) {
            out.append("Q").append(question.getOrdinal()).append(' ')
                    .append(question.getType() == QuestionType.WRITTEN
                            ? "(written)" : "(multiple choice)")
                    .append('\n')
                    .append("  Asked: ").append(indented(question.getBody())).append('\n');

            if (question.getType() == QuestionType.WRITTEN) {
                renderWritten(out, question);
            }
            else {
                renderMultipleChoice(out, question);
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static void renderWritten(StringBuilder out, Question question) {
        out.append("  A full answer covers: ").append(indented(question.getRubric())).append('\n');
        if (question.getModelAnswer() != null) {
            out.append("  Model answer: ").append(indented(question.getModelAnswer())).append('\n');
        }
        out.append("  Learner wrote: ")
                .append(question.getAnswerWritten() == null || question.getAnswerWritten().isBlank()
                        ? "(left blank)"
                        : indented(question.getAnswerWritten()))
                .append('\n');
    }

    private static void renderMultipleChoice(StringBuilder out, Question question) {
        Set<Choice> choices = question.getChoices();
        String selected = question.getAnswerMc();

        out.append("  Choices:\n");
        for (Choice choice : choices) {
            boolean picked = selected != null
                    && choice.getChoiceId() != null
                    && selected.contains(choice.getChoiceId().toString());
            out.append("    ")
                    .append(picked ? "[chosen] " : "         ")
                    .append(choice.isCorrect() ? "(correct) " : "(wrong)   ")
                    .append(indented(choice.getBody()));
            if (choice.getExplanation() != null && !choice.getExplanation().isBlank()) {
                out.append("\n              why: ").append(indented(choice.getExplanation()));
            }
            out.append('\n');
        }
        if (selected == null || selected.isBlank()) {
            out.append("    Learner did not answer this question.\n");
        }
    }

    /** Keeps a multi-line body from breaking the one-entry-per-line layout. */
    private static String indented(String value) {
        return value == null ? "" : value.replace("\n", "\n      ");
    }
}
