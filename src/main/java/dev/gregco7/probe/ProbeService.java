package dev.gregco7.probe;

import dev.gregco7.kc.Choice;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProbeService {

    private final ProbeRepository probes;

    ProbeService(ProbeRepository probes) {
        this.probes = probes;
    }

    @Transactional
    public Probe create(Probe planned) {
        return probes.save(planned);
    }

    /**
     * Records what the learner gave and hands back the sat probe, with its
     * questions and their choices loaded.
     *
     * <p>Everything is read outside this transaction afterwards — the planning
     * calls that read it run for minutes — so the associations are walked here
     * while the session is still open rather than left to fail lazily later.
     *
     * @param answers answers to some or all of the probe's questions; a question
     *                left out stays unanswered, which is itself evidence the
     *                planner reads
     */
    @Transactional
    public Probe recordAnswers(UUID probeId, List<ProbeAnswer> answers) {
        Probe probe = probes.findById(probeId).orElseThrow(() -> new ProbeNotFoundException(probeId));

        Map<UUID, ProbeAnswer> byQuestion = new HashMap<>();
        for (ProbeAnswer answer : answers != null ? answers : List.<ProbeAnswer>of()) {
            if (answer != null && answer.questionId() != null) {
                byQuestion.put(answer.questionId(), answer);
            }
        }

        for (Question question : probe.getQuestions()) {
            // Touched whether or not it was answered, so the whole graph is loaded
            // before the entity is read outside this transaction.
            Set<Choice> choices = question.getChoices();
            ProbeAnswer answer = byQuestion.get(question.getQuestionId());
            if (answer == null) {
                continue;
            }
            if (question.getType() == QuestionType.WRITTEN) {
                // Left ungraded: a probe is not marked question by question. The
                // planner reads the learner's own words, where the useful signal is.
                question.submitWrittenAnswer(answer.written(), null);
            }
            else {
                recordMultipleChoice(question, choices, answer);
            }
        }

        return probe;
    }

    private void recordMultipleChoice(Question question, Set<Choice> choices, ProbeAnswer answer) {
        Set<UUID> valid = choices.stream().map(Choice::getChoiceId).collect(Collectors.toSet());
        Set<UUID> picked = new LinkedHashSet<>();
        for (UUID choiceId : answer.choiceIds() != null ? answer.choiceIds() : List.<UUID>of()) {
            if (valid.contains(choiceId)) {
                picked.add(choiceId);
            }
        }
        if (picked.isEmpty()) {
            return;
        }

        Set<UUID> correct = choices.stream()
                .filter(Choice::isCorrect)
                .map(Choice::getChoiceId)
                .collect(Collectors.toSet());

        // kc.questions.answer_mc takes the selections concatenated into one column.
        String recorded = picked.stream().map(UUID::toString).collect(Collectors.joining(","));
        question.submitMcAnswer(recorded, picked.equals(correct));
    }
}
