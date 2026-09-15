package dev.gregco7.session;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("No session with id " + sessionId);
    }
}
