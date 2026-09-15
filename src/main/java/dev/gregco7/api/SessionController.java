package dev.gregco7.api;

import dev.gregco7.plan.SessionPlanner;
import dev.gregco7.probe.Probe;
import dev.gregco7.probe.ProbeService;
import dev.gregco7.session.Session;
import dev.gregco7.session.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
class SessionController {

    private final SessionPlanner planner;
    private final ProbeService probes;
    private final SessionService sessions;

    SessionController(SessionPlanner planner, ProbeService probes, SessionService sessions) {
        this.planner = planner;
        this.probes = probes;
        this.sessions = sessions;
    }

    @GetMapping
    List<SessionCard> list() {
        return sessions.list().stream().map(SessionCard::of).toList();
    }

    @GetMapping("/{sessionId}")
    SessionView get(@PathVariable UUID sessionId) {
        return SessionView.of(sessions.get(sessionId));
    }

    /**
     * Plans a whole session and saves it.
     *
     * <p><strong>This runs for minutes, not seconds.</strong> It is one Claude
     * call for the outline plus one per lesson, so a nine-lesson session is ten
     * calls. The client needs a timeout to match and something on screen that
     * survives the wait.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SessionView create(@Valid @RequestBody ApiRequests.CreateSession request) {
        Probe probe = request.probeId() == null
                ? null
                : probes.recordAnswers(request.probeId(), ApiRequests.toSubmitted(request.answers()));

        Session planned = planner.plan(
                request.topic(), request.lvl(), nodeCount(request), probe);

        return SessionView.of(sessions.createFromPlan(planned));
    }

    /** Higher goal proficiency means a finer breakdown of the same topic. */
    private static int nodeCount(ApiRequests.CreateSession request) {
        if (request.numNodes() != null) {
            return request.numNodes();
        }
        return switch (request.lvl()) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            default -> 9;
        };
    }
}
