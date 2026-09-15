package dev.gregco7.api;

import dev.gregco7.plan.ProbePlanner;
import dev.gregco7.probe.ProbeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/probes")
class ProbeController {

    private final ProbePlanner planner;
    private final ProbeService probes;

    ProbeController(ProbePlanner planner, ProbeService probes) {
        this.planner = planner;
        this.probes = probes;
    }

    /**
     * Has Claude write the diagnostic that locates the learner on this topic.
     *
     * <p>Slow: one Claude call, tens of seconds. The response carries no answer
     * key, so it can go straight to the screen.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProbeView create(@Valid @RequestBody ApiRequests.CreateProbe request) {
        return ProbeView.of(probes.create(planner.plan(request.topic(), request.lvl())));
    }

    @GetMapping("/{probeId}")
    ProbeView get(@PathVariable UUID probeId) {
        return ProbeView.of(probes.get(probeId));
    }
}
