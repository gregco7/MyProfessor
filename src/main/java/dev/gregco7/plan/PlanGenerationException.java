package dev.gregco7.plan;

/**
 * Claude answered, but with something the session model cannot be built from —
 * or did not answer at all. The shell catches this and reports the message to
 * the learner; the cause is logged to the file.
 */
public class PlanGenerationException extends RuntimeException {

    public PlanGenerationException(String message) {
        super(message);
    }

    public PlanGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
