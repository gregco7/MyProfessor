package dev.gregco7.probe;

import dev.gregco7.kc.Answers;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.SubmittedAnswer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Probe get(UUID probeId) {
        Probe probe = probes.findById(probeId).orElseThrow(() -> new ProbeNotFoundException(probeId));
        probe.getQuestions().forEach(question -> question.getChoices().size());
        return probe;
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
    public Probe recordAnswers(UUID probeId, List<SubmittedAnswer> answers) {
        Probe probe = probes.findById(probeId).orElseThrow(() -> new ProbeNotFoundException(probeId));

        Map<UUID, SubmittedAnswer> byQuestion = new HashMap<>();
        for (SubmittedAnswer answer : answers != null ? answers : List.<SubmittedAnswer>of()) {
            if (answer != null && answer.questionId() != null) {
                byQuestion.put(answer.questionId(), answer);
            }
        }

        for (Question question : probe.getQuestions()) {
            // Touched whether or not it was answered, so the whole graph is loaded
            // before the entity is read outside this transaction.
            question.getChoices().size();
            SubmittedAnswer answer = byQuestion.get(question.getQuestionId());
            if (answer != null) {
                Answers.record(question, answer);
            }
        }

        return probe;
    }
}
