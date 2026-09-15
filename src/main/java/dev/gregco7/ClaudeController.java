package dev.gregco7;
import dev.gregco7.plan.ProbePlanner;
import dev.gregco7.plan.SessionPlanner;
import dev.gregco7.probe.Probe;
import dev.gregco7.probe.ProbeResponse;
import dev.gregco7.probe.ProbeService;
import dev.gregco7.session.Session;
import dev.gregco7.session.SessionPlanResponse;
import dev.gregco7.session.SessionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Building a session is two calls, because a course is only worth the learner's
 * time if it starts where they actually are.
 *
 * <p>First {@link #createProbe} has Claude write a diagnostic for the topic. The
 * learner sits it, and their answers come back with {@link #createSession},
 * which has Claude read them before choosing a single lesson. Skipping the probe
 * is allowed and gives a session planned from the topic alone — which necessarily
 * starts at the beginning of it.
 */
@RestController

public class ClaudeController {

    private final ProbePlanner probePlanner;
    private final ProbeService probes;
    private final SessionPlanner planner;
    private final SessionService sessions;

    ClaudeController(ProbePlanner probePlanner, ProbeService probes,
                     SessionPlanner planner, SessionService sessions) {
        this.probePlanner = probePlanner;
        this.probes = probes;
        this.planner = planner;
        this.sessions = sessions;
    }

    /**
     * Has Claude write the diagnostic that locates the learner on this topic: a
     * short ascending sweep of questions meant to bracket them between the last
     * thing they get right and the first thing they do not.
     *
     * <p>The response carries no answer key — not which choices are correct, not
     * the rubrics, not the hints — so it can be handed straight to the learner.
     */
    @PostMapping("/api/claude/probes")
    @ResponseStatus(HttpStatus.CREATED)
    public ProbeResponse createProbe(@Valid @RequestBody GenerateProbeRequest request) {
        Probe planned = probePlanner.plan(request.topic(), request.lvl());
        return ProbeResponse.of(probes.create(planned));
    }

    /**
     * Has Claude plan a whole session for the requested topic — the lessons and
     * their prerequisite order, the study material under each, and the questions
     * that decide whether the learner has passed it — then saves it in one go.
     *
     * <p>When the request carries a sat probe, the answers are recorded against
     * it and read first: the session then begins at the edge of what the learner
     * already holds rather than at the start of the topic, and the reading that
     * produced that decision comes back on the response.
     *
     * <p>This is a slow call by nature — a request to Claude per lesson plus the
     * outline — so expect it to run for minutes rather than seconds.
     */
    @PostMapping("/api/claude/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionPlanResponse createSession(@Valid @RequestBody GenerateSessionRequest request) {
        Probe probe = request.probeId() == null
                ? null
                : probes.recordAnswers(request.probeId(), request.answers());

        Session planned = planner.plan(
                request.topic(), request.lvl(), request.resolvedNodeCount(), probe);

        return SessionPlanResponse.of(sessions.createFromPlan(planned));
    }
}
