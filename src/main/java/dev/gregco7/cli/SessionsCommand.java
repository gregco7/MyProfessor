package dev.gregco7.cli;

import dev.gregco7.plan.Proficiency;
import dev.gregco7.session.SessionService;
import dev.gregco7.session.SessionSummary;
import org.springframework.stereotype.Component;

import java.util.List;

/** {@code /sessions} — what has been built so far. */
@Component
class SessionsCommand implements ShellCommand {

    private final SessionService sessions;

    SessionsCommand(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public String name() {
        return "/sessions";
    }

    @Override
    public String usage() {
        return "/sessions";
    }

    @Override
    public String description() {
        return "list the sessions on this database";
    }

    @Override
    public void run(List<String> args, Term term) {
        List<SessionSummary> all = sessions.list();
        term.blank();
        if (all.isEmpty()) {
            term.info("  Nothing yet. Start one with /init_learn 'a concept' competent");
            return;
        }

        for (SessionSummary session : all) {
            term.println("  " + Ansi.BOLD + session.topic() + Ansi.RESET
                    + Ansi.DIM + "  " + session.numNodes() + " lessons · "
                    + Proficiency.label(session.lvl())
                    + (session.probed() ? " · diagnostic sat" : " · no diagnostic")
                    + Ansi.RESET);
            term.info("    " + session.sessionId());
        }
    }
}
