package dev.gregco7.api;

import dev.gregco7.kc.NodeNotFoundException;
import dev.gregco7.plan.PlanGenerationException;
import dev.gregco7.probe.ProbeNotFoundException;
import dev.gregco7.session.SessionNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns the domain's failures into responses the dashboard can branch on.
 *
 * <p>Everything comes back as RFC 9457 ProblemDetail, so the client has one
 * shape to parse whatever went wrong.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Log logger = LogFactory.getLog(ApiExceptionHandler.class);

    @ExceptionHandler({ProbeNotFoundException.class, NodeNotFoundException.class,
            SessionNotFoundException.class})
    ProblemDetail notFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NotAttemptedException.class)
    ProblemDetail notAttempted(NotAttemptedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Claude refused, timed out, ran out of credit, or answered unusably. */
    @ExceptionHandler(PlanGenerationException.class)
    ProblemDetail upstream(PlanGenerationException ex) {
        logger.error("Planning failed", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }
}
