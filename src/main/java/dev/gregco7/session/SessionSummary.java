package dev.gregco7.session;

import java.time.Instant;
import java.util.UUID;

/**
 * One line of the session list. Flattened inside the transaction that reads it,
 * so the caller can print it with no persistence context open.
 */
public record SessionSummary(
        UUID sessionId, String topic, short lvl, int numNodes, boolean probed, Instant createdAt) {}
