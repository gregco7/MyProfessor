package dev.gregco7.probe;

import java.util.UUID;

public class ProbeNotFoundException extends RuntimeException {

    public ProbeNotFoundException(UUID probeId) {
        super("No probe with id " + probeId);
    }
}
