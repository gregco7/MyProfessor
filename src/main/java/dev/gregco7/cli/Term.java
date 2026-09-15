package dev.gregco7.cli;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Everything the shell does to the screen and the keyboard, in one place.
 *
 * <p>Reading returns null at end of input rather than throwing, because a piped
 * or closed stdin is a normal way for this program to end — the caller treats it
 * the same as someone typing {@code /exit}.
 */
@Component
public class Term {

    /**
     * Cursor keys and their relatives, as they arrive on stdin: CSI
     * ({@code ESC [ … }), SS3 ({@code ESC O x}), and a bare ESC.
     *
     * <p>Only used on the cooked fallback path. With the terminal in raw mode
     * these are discarded a byte at a time as they are read, before anything is
     * echoed, which is the only way to keep them off the screen entirely.
     */
    private static final Pattern CONTROL_SEQUENCES =
            Pattern.compile("\033\\[[0-?]*[ -/]*[@-~]|\033O[@-~]|\033");

    private static final int CTRL_C = 3;
    private static final int CTRL_D = 4;
    private static final int CTRL_U = 21;
    private static final int BACKSPACE = 8;
    private static final int DELETE = 127;
    private static final int ESC = 27;

    private final PrintStream out = System.out;
    private final BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    /** Set once, by the shell, after it has taken the terminal. */
    private volatile boolean raw;

    void setRaw(boolean raw) {
        this.raw = raw;
    }

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
            return raw ? readLineRaw() : readLineCooked();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String readLineCooked() throws IOException {
        String line = in.readLine();
        return line == null ? null : CONTROL_SEQUENCES.matcher(line).replaceAll("").strip();
    }

    /**
     * Reads a line while the terminal is echoing nothing of its own.
     *
     * <p>Every character that reaches the screen is written here, so anything
     * the terminal invents — a scroll turned into cursor keys, a stray escape
     * from some other program — is simply dropped and never seen. Editing is
     * deliberately minimal: backspace and kill-line. There is no history; an
     * arrow key is swallowed rather than recalling anything, which is honest
     * about what this does rather than appearing to offer more.
     */
    private String readLineRaw() throws IOException {
        StringBuilder line = new StringBuilder();
        while (true) {
            int ch = in.read();
            switch (ch) {
                case -1, CTRL_C -> {
                    return null;
                }
                case '\r', '\n' -> {
                    blank();
                    return line.toString().strip();
                }
                case CTRL_D -> {
                    // End of input only on an empty line, as a shell does.
                    if (line.isEmpty()) {
                        return null;
                    }
                }
                case BACKSPACE, DELETE -> {
                    if (!line.isEmpty()) {
                        line.setLength(line.length() - 1);
                        // Back over the character, paint a space, back again.
                        print("\b \b");
                    }
                }
                case CTRL_U -> {
                    print("\b \b".repeat(line.length()));
                    line.setLength(0);
                }
                case ESC -> discardEscape();
                default -> {
                    // Printable only. Anything else is a control code nobody
                    // typed on purpose and nothing here knows what to do with.
                    if (ch >= 32) {
                        line.append((char) ch);
                        print(String.valueOf((char) ch));
                    }
                }
            }
        }
    }

    /**
     * Swallows the rest of an escape sequence without echoing any of it.
     *
     * <p>Guarded by {@code ready()} throughout: a sequence arrives as one burst,
     * so anything still buffered belongs to it, while a lone ESC — someone
     * actually pressing the key — leaves nothing behind and must not block
     * waiting for a continuation that is never coming.
     */
    private void discardEscape() throws IOException {
        if (!in.ready()) {
            return;
        }
        int next = in.read();
        if (next == '[') {
            while (in.ready()) {
                int param = in.read();
                // A CSI ends at its final byte; the rest is parameters.
                if (param >= '@' && param <= '~') {
                    return;
                }
            }
        }
        else if (next == 'O' && in.ready()) {
            in.read();
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
