package dev.gregco7.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessions;

    SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    public record CreateSessionRequest(
            @NotBlank String topic,
            @Min(1) @Max(4) short lvl) {}

    public record SessionResponse(UUID sessionId, String topic, short lvl) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@Valid @RequestBody CreateSessionRequest request) {
        Session session = sessions.create(request.topic(), request.lvl());
        return new SessionResponse(
                session.getSessionId(), session.getTopic(), session.getGoalProficiency());
    }
}
