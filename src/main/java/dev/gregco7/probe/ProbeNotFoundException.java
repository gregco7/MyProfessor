package dev.gregco7.probe;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProbeNotFoundException extends RuntimeException {

    public ProbeNotFoundException(UUID probeId) {
        super("No probe with id " + probeId);
    }
}
