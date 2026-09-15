package dev.gregco7.cli;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The application stage: takes over the terminal on startup, reads commands
 * until told to leave, and hands the terminal back as it found it.
 *
 * <p>Only present under the {@code cli} profile. Without it the process is the
 * HTTP API and nothing here is loaded, so starting the server can never end up
 * blocked on a prompt nobody is watching.
 */
@Component
@Profile("cli")
public class AiLearnShell implements ApplicationRunner {

    private static final Log logger = LogFactory.getLog(AiLearnShell.class);

    private static final String PROMPT = Ansi.BOLD + Ansi.CYAN + "ailearn" + Ansi.RESET
            + Ansi.DIM + " ›" + Ansi.RESET + " ";

    private final Term term;
    private final Banner banner;
    private final Map<String, ShellCommand> commands = new LinkedHashMap<>();
    private final List<ShellCommand> listed;

    AiLearnShell(Term term, Banner banner, List<ShellCommand> commands) {
        this.term = term;
        this.banner = banner;
        commands.stream()
                .sorted(Comparator.comparing(ShellCommand::name))
                .forEach(command -> this.commands.put(command.name(), command));

        // /help and /exit are intercepted by the loop below rather than dispatched,
        // because one needs the command list and the other needs to end it. They
        // are still listed, since a command the banner hides may as well not exist.
        this.listed = Stream.concat(
                this.commands.values().stream(),
                Stream.of(
                        new Builtin("/help", "/help", "show this list again"),
                        new Builtin("/exit", "/exit", "leave (Ctrl-D does the same)")))
                .toList();
    }

    /** A listed command the loop handles directly; {@link #run} is never reached. */
    private record Builtin(String name, String usage, String description) implements ShellCommand {
        @Override
        public void run(List<String> args, Term term) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        term.print(Ansi.enterApplicationScreen());
        // Restores the screen even if the JVM is killed mid-session; without it a
        // Ctrl-C would leave the terminal stuck in the alternate buffer.
        Thread restore = new Thread(() -> term.print(Ansi.exitApplicationScreen()));
        Runtime.getRuntime().addShutdownHook(restore);

        try {
            banner.draw(term, listed);
            loop();
        }
        finally {
            Runtime.getRuntime().removeShutdownHook(restore);
            term.print(Ansi.exitApplicationScreen());
        }
    }

    private void loop() {
        while (true) {
            String line = term.readLine(PROMPT);
            if (line == null) {
                // stdin closed: the same intent as /exit, just expressed by pipe.
                return;
            }

            CommandLine typed = CommandLine.parse(line);
            if (typed == null) {
                continue;
            }
            if (typed.name().equals("/exit") || typed.name().equals("/quit")) {
                term.info("Bye.");
                return;
            }
            if (typed.name().equals("/help")) {
                banner.draw(term, listed);
                continue;
            }

            dispatch(typed);
        }
    }

    private void dispatch(CommandLine typed) {
        ShellCommand command = commands.get(typed.name());
        if (command == null) {
            term.error("Unknown command: " + typed.name());
            term.info("Type /help to see what this accepts.");
            term.blank();
            return;
        }

        try {
            command.run(typed.args(), term);
        }
        catch (IllegalArgumentException ex) {
            // Bad input, not a bug: say what was wrong and how it should read.
            term.error(ex.getMessage());
            term.info("usage: " + command.usage());
        }
        catch (RuntimeException ex) {
            // Anything else is worth the full trace, but in the log rather than
            // sprayed over the screen the learner is working in.
            logger.error("Command failed: " + typed.name(), ex);
            term.error(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            term.info("Details in logs/myprofessor.log");
        }
        term.blank();
    }
}
