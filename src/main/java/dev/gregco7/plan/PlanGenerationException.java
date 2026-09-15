package dev.gregco7.plan;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Claude answered, but with something the session model cannot be built from.
 * That is an upstream failure rather than a bad request, so it surfaces as 502.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class PlanGenerationException extends RuntimeException {

    public PlanGenerationException(String message) {
        super(message);
    }

    public PlanGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
