package dev.gregco7.kc;

import java.util.UUID;

/**
 * A lesson was sat before the lessons it depends on were passed.
 *
 * <p>The database refuses this write on its own, so this exists for the message
 * rather than for the rule: a trigger rejection surfaces as an integrity
 * violation naming a constraint, which is not something to show a learner.
 */
public class LockedNodeException extends RuntimeException {

    public LockedNodeException(UUID nodeId) {
        super("Lesson " + nodeId + " is locked; its prerequisite lessons have not been passed yet.");
    }
}
