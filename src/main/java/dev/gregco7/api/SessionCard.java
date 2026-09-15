package dev.gregco7.api;

import dev.gregco7.plan.Proficiency;
import dev.gregco7.session.SessionSummary;

import java.time.Instant;
import java.util.UUID;

/**
 * A session as it appears in a list. Newest first.
 *
 * @param probed false when the session was planned from the topic alone, which
 *               means it starts at the beginning of that topic rather than at
 *               the edge of what the learner knows
 */
public record SessionCard(
        UUID sessionId,
        String topic,
        short lvl,
        String proficiency,
        int numNodes,
        boolean probed,
        Instant createdAt) {

    public static SessionCard of(SessionSummary summary) {
        return new SessionCard(
                summary.sessionId(), summary.topic(), summary.lvl(),
                Proficiency.label(summary.lvl()), summary.numNodes(),
                summary.probed(), summary.createdAt());
    }
}
