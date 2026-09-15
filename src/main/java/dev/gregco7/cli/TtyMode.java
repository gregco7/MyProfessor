package dev.gregco7.cli;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Takes the terminal's echo away, and gives it back.
 *
 * <p>The reason is the trackpad. In the alternate screen buffer a scroll is
 * translated by the terminal into cursor-key escapes and delivered as input;
 * asking it not to do that (mode 1007) works in some terminals and is quietly
 * ignored by others. The escapes are then painted on screen by the tty itself,
 * before any Java code has seen a byte — so nothing the program does to its own
 * input can erase them.
 *
 * <p>Turning off {@code echo} removes the only thing that was drawing them.
 * With {@code -icanon} as well, input arrives a byte at a time and the shell
 * decides what appears: printable characters are echoed back, escape sequences
 * are swallowed. What the terminal injects then has no way to reach the screen,
 * whatever it thinks of mode 1007.
 *
 * <p>{@code ISIG} is deliberately left alone, so Ctrl-C still interrupts.
 */
final class TtyMode {

    private static final Log logger = LogFactory.getLog(TtyMode.class);

    /** The exact settings as they were, in the opaque form {@code stty -g} emits. */
    private volatile String saved;

    /**
     * @return true if raw mode is on and the caller must do its own echoing;
     *         false leaves the terminal cooked and the caller on its fallback
     *         path, which is also what happens when input is a pipe
     */
    boolean enterRaw() {
        if (System.console() == null) {
            return false;
        }
        String original = capture("stty", "-g");
        if (original == null) {
            logger.warn("Could not read terminal settings; leaving the terminal cooked.");
            return false;
        }
        if (capture("stty", "-echo", "-icanon", "min", "1", "time", "0") == null) {
            logger.warn("Could not put the terminal into raw mode; leaving it cooked.");
            return false;
        }
        this.saved = original;
        return true;
    }

    /**
     * Puts the terminal back exactly as it was.
     *
     * <p>Idempotent, because it is called from both the normal exit path and a
     * shutdown hook, and either may run first. Leaving a terminal with echo off
     * is the worst outcome available here, so it is worth calling twice.
     */
    void restore() {
        String original = this.saved;
        this.saved = null;
        if (original != null) {
            capture("stty", original);
        }
    }

    /** Runs stty against this process's own terminal. @return its output, or null if it failed */
    private static String capture(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    // stty acts on the terminal attached to its stdin, which has
                    // to be ours rather than a pipe.
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            return process.waitFor() == 0 ? output : null;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }
}
