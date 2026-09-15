package dev.gregco7.api;

import java.util.UUID;

/** The answer key was asked for before the lesson was sat. */
class NotAttemptedException extends RuntimeException {

    NotAttemptedException(UUID nodeId) {
        super("Lesson " + nodeId + " has not been attempted yet; there is nothing to review.");
    }
}
