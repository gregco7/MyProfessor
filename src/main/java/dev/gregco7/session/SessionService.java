package dev.gregco7.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessions;

    SessionService(SessionRepository sessions) {
        this.sessions = sessions;
    }

    @Transactional
    public Session create(String topic, short lvl) {
        return sessions.save(new Session(topic, lvl));
    }
}
