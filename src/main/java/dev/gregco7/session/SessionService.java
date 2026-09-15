package dev.gregco7.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    /**
     * One session with its lesson tree loaded — sections and questions counted,
     * prerequisite edges walked — so the caller can render it with no session open.
     */
    @Transactional(readOnly = true)
    public Session get(UUID sessionId) {
        Session session = sessions.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        session.getNodes().forEach(node -> {
            node.getLearnSections().size();
            node.getQuestions().size();
            node.getPrerequisites().size();
        });
        if (session.getProbe() != null) {
            session.getProbe().getProbeId();
        }
        return session;
    }

    /**
     * Every session, flattened for listing. The counts are read here so the rows
     * leave this method with nothing left to load lazily.
     */
    @Transactional(readOnly = true)
    public List<SessionSummary> list() {
        return sessions.findAll().stream()
                .map(session -> new SessionSummary(
                        session.getSessionId(),
                        session.getTopic(),
                        session.getGoalProficiency(),
                        session.getNodes().size(),
                        session.getProbe() != null,
                        session.getCreatedAt()))
                .sorted(java.util.Comparator.comparing(SessionSummary::createdAt).reversed())
                .toList();
    }
}
