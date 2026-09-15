package dev.gregco7.cli;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Everything the shell does to the screen and the keyboard, in one place.
 *
 * <p>Reading returns null at end of input rather than throwing, because a piped
 * or closed stdin is a normal way for this program to end — the caller treats it
 * the same as someone typing {@code /exit}.
 */
@Component
public class Term {

    private final PrintStream out = System.out;
    private final BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public void print(String text) {
        out.print(text);
        out.flush();
    }

    public void println(String text) {
        out.println(text);
    }

    public void blank() {
        out.println();
    }

    /** @return what was typed, or null if input ended */
    public String readLine(String prompt) {
        print(prompt);
        try {
            String line = in.readLine();
            return line == null ? null : line.strip();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void info(String text) {
        println(Ansi.DIM + text + Ansi.RESET);
    }

    public void good(String text) {
        println(Ansi.GREEN + text + Ansi.RESET);
    }

    public void warn(String text) {
        println(Ansi.YELLOW + text + Ansi.RESET);
    }

    public void error(String text) {
        println(Ansi.RED + text + Ansi.RESET);
    }

    public void heading(String text) {
        println(Ansi.BOLD + text + Ansi.RESET);
    }

    /**
     * Runs work that takes long enough to look like a hang, showing a spinner
     * until it finishes. Planning a session is minutes of waiting on Claude with
     * nothing to print, and a frozen terminal is indistinguishable from a crash.
     *
     * <p>The spinner thread is a daemon and only ever rewrites its own line, so
     * it cannot outlive the work or disturb what has already been printed.
     */
    public <T> T whileWaiting(String message, java.util.function.Supplier<T> work) {
        if (!Ansi.enabled()) {
            info(message);
            return work.get();
        }

        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        Thread spinner = Thread.ofVirtual().unstarted(() -> {
            int frame = 0;
            try {
                while (true) {
                    print(Ansi.clearLine() + Ansi.CYAN + frames[frame++ % frames.length]
                            + Ansi.RESET + " " + message);
                    Thread.sleep(80);
                }
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        spinner.start();
        try {
            return work.get();
        }
        finally {
            spinner.interrupt();
            try {
                spinner.join(200);
            }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            print(Ansi.clearLine());
        }
    }
}
