package dev.gregco7.kc;

import java.util.UUID;

public class NodeNotFoundException extends RuntimeException {

    public NodeNotFoundException(UUID nodeId) {
        super("No lesson with id " + nodeId);
    }
}
