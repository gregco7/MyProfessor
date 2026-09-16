package dev.gregco7.kc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NodeService {

    private final NodeRepository nodes;

    NodeService(NodeRepository nodes) {
        this.nodes = nodes;
    }

    @Transactional(readOnly = true)
    public Node get(UUID nodeId) {
        Node node = nodes.findById(nodeId).orElseThrow(() -> new NodeNotFoundException(nodeId));
        // Walked here so the caller can read the lesson with no session open.
        node.getLearnSections().size();
        node.getQuestions().forEach(question -> question.getChoices().size());
        return node;
    }

    /**
     * Records a sitting of the lesson's questions.
     *
     * <p>The attempt is dated whatever the outcome — that is the point of the
     * timestamp. Questions the learner skipped are left unanswered and count
     * against the total, since not answering is not the same as being right and
     * the pass requirement is measured against the whole lesson.
     *
     * <p>Every answer on the lesson is cleared first. An attempt replaces the
     * previous one rather than being merged into it, which is what keeps the
     * score this call returns and the answers the review shows in agreement.
     *
     * <p>Only multiple choice is marked. Written answers are stored unmarked
     * until there is a review pass to judge them, so they cannot currently count
     * toward a pass — worth knowing when reading a score back.
     *
     * <p>A lesson whose prerequisites are unpassed is refused. The database
     * refuses it too, and that is the enforcement; this check is here so the
     * refusal arrives as a sentence about the lesson rather than as a trigger
     * firing halfway through flushing the attempt.
     */
    @Transactional
    public AttemptResult attempt(UUID nodeId, List<SubmittedAnswer> answers) {
        Node node = nodes.findById(nodeId).orElseThrow(() -> new NodeNotFoundException(nodeId));

        if (node.getPrerequisites().stream().anyMatch(prereq -> !prereq.isPassed())) {
            throw new LockedNodeException(nodeId);
        }

        Map<UUID, SubmittedAnswer> byQuestion = new HashMap<>();
        for (SubmittedAnswer answer : answers != null ? answers : List.<SubmittedAnswer>of()) {
            if (answer != null && answer.questionId() != null) {
                byQuestion.put(answer.questionId(), answer);
            }
        }

        List<AttemptResult.QuestionOutcome> outcomes = new ArrayList<>();
        int correct = 0;

        for (Question question : node.getQuestions()) {
            question.getChoices().size();
            // Cleared before recording, so this attempt stands on its own rather
            // than on top of whatever a previous sitting left behind.
            question.clearAnswer();
            SubmittedAnswer answer = byQuestion.get(question.getQuestionId());

            boolean answered = answer != null;
            boolean right = answered && Answers.record(question, answer);
            if (right) {
                correct++;
            }
            outcomes.add(new AttemptResult.QuestionOutcome(
                    question.getQuestionId(),
                    question.getOrdinal(),
                    question.getType(),
                    answered,
                    question.getType() == QuestionType.MULTIPLE_CHOICE && answered,
                    right));
        }

        node.recordAttempt(correct >= node.getPassRequirement());

        return new AttemptResult(
                node.getNodeId(),
                correct,
                node.getQuestions().size(),
                node.getPassRequirement(),
                node.isPassed(),
                node.getLastAttemptedTime(),
                outcomes);
    }
}
