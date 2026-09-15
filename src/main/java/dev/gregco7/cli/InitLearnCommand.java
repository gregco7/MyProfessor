package dev.gregco7.cli;

import dev.gregco7.plan.ProbePlanner;
import dev.gregco7.plan.Proficiency;
import dev.gregco7.probe.Probe;
import dev.gregco7.probe.ProbeService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@code /init_learn 'concept' 'proficiency'} — writes the diagnostic and hands
 * it to the browser.
 *
 * <p>The diagnostic is sat in the web app, as an ordinary quiz, because that is
 * what it is: the same rows, the same shapes, the same way of answering. Putting
 * it at a terminal prompt made it a different and worse thing — no going back to
 * change an answer, prose typed on one line, code in the questions unreadable.
 *
 * <p>Everything after the hand-off happens in the browser: answering, then the
 * planning it kicks off, then the session it lands in. The shell's part is over
 * once the link is open, so it returns to the prompt rather than blocking on
 * work it is no longer doing.
 */
@Component
class InitLearnCommand implements ShellCommand {

    private final ProbePlanner probePlanner;
    private final ProbeService probes;
    private final ServerAddress server;

    InitLearnCommand(ProbePlanner probePlanner, ProbeService probes, ServerAddress server) {
        this.probePlanner = probePlanner;
        this.probes = probes;
        this.server = server;
    }

    @Override
    public String name() {
        return "/init_learn";
    }

    @Override
    public String usage() {
        return "/init_learn 'concept' 'proficiency'";
    }

    @Override
    public String description() {
        return "write a diagnostic and open it in the browser";
    }

    @Override
    public void run(List<String> args, Term term) {
        if (args.size() != 2) {
            throw new IllegalArgumentException(
                    "expected a concept and a proficiency, got " + args.size() + " argument(s)");
        }
        String concept = args.get(0);
        if (concept.isBlank()) {
            throw new IllegalArgumentException("the concept cannot be empty");
        }
        short lvl = Proficiency.parse(args.get(1));

        if (!server.isReady()) {
            throw new IllegalStateException(
                    "The web app is not up, so there is nowhere to send the diagnostic.");
        }

        term.blank();
        term.heading("  " + concept);
        term.info("  aiming for " + Proficiency.label(lvl) + " (" + lvl + " of 4)");
        term.blank();

        Probe probe = term.whileWaiting(
                "Writing a diagnostic to find where you already stand...",
                () -> probes.create(probePlanner.plan(concept, lvl)));

        String url = server.probeUrl(probe.getProbeId());

        term.good("  Diagnostic ready — " + probe.getQuestions().size() + " questions.");
        term.blank();
        term.println("  " + url);
        term.blank();

        if (Browser.open(url)) {
            term.info("  Opened in your browser. Answer it there, and the session is");
            term.info("  planned from what it shows. That takes a few minutes.");
        }
        else {
            term.warn("  Could not open a browser for you — paste the link above.");
        }
        term.blank();
        // This shell is the server. Saying so here is the difference between a
        // page that works and one that dies halfway through the diagnostic.
        term.warn("  Keep this shell open — it is serving that page.");
        term.info("  /sessions lists everything once the session lands.");
    }
}
