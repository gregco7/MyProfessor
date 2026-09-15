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

    /**
     * Saves a session that was built in full elsewhere, along with everything
     * cascaded under it. Kept separate from {@link #create} so the planning calls,
     * which run for minutes, happen outside the transaction rather than holding a
     * connection open for their duration.
     */
    @Transactional
    public Session createFromPlan(Session planned) {
        return sessions.save(planned);
    }
}
