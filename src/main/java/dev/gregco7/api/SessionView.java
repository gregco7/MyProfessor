package dev.gregco7.api;

import dev.gregco7.kc.Node;
import dev.gregco7.plan.Proficiency;
import dev.gregco7.session.Session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One session and the shape of its lesson tree — enough to draw the map, not
 * enough to study from. The material and the questions come from
 * {@code GET /api/nodes/{nodeId}} one lesson at a time.
 *
 * @param assessment what the diagnostic showed about this learner, which is why
 *                   the tree starts where it does; null when no diagnostic was sat
 */
public record SessionView(
        UUID sessionId,
        String topic,
        short lvl,
        String proficiency,
        String assessment,
        UUID probeId,
        Instant createdAt,
        List<NodeCard> nodes) {

    /**
     * @param prerequisiteIds other nodes in this same session that must be passed
     *                        first; empty means the lesson is available now
     * @param lastAttemptedTime null until the learner first sits it
     */
    public record NodeCard(
            UUID nodeId,
            String subtopic,
            short passRequirement,
            boolean passed,
            Instant lastAttemptedTime,
            int numSections,
            int numQuestions,
            List<UUID> prerequisiteIds) {}

    public static SessionView of(Session session) {
        return new SessionView(
                session.getSessionId(),
                session.getTopic(),
                session.getGoalProficiency(),
                Proficiency.label(session.getGoalProficiency()),
                session.getAssessment(),
                session.getProbe() != null ? session.getProbe().getProbeId() : null,
                session.getCreatedAt(),
                session.getNodes().stream().map(SessionView::card).toList());
    }

    private static NodeCard card(Node node) {
        return new NodeCard(
                node.getNodeId(),
                node.getSubtopic(),
                node.getPassRequirement(),
                node.isPassed(),
                node.getLastAttemptedTime(),
                node.getLearnSections().size(),
                node.getQuestions().size(),
                node.getPrerequisites().stream().map(Node::getNodeId).toList());
    }
}
