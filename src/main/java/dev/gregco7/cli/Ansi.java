package dev.gregco7.cli;

/**
 * The escape sequences the shell uses, and the single decision about whether to
 * emit them at all.
 *
 * <p>When output is piped or redirected there is no terminal to style, and the
 * codes would end up in the file as literal junk. Everything here collapses to
 * plain text in that case, so the same rendering code serves both.
 */
final class Ansi {

    private static final boolean ENABLED = System.console() != null;

    static final String RESET = code("\033[0m");
    static final String BOLD = code("\033[1m");
    static final String DIM = code("\033[2m");
    static final String CYAN = code("\033[36m");
    static final String GREEN = code("\033[32m");
    static final String YELLOW = code("\033[33m");
    static final String RED = code("\033[31m");

    private Ansi() {}

    static boolean enabled() {
        return ENABLED;
    }

    /**
     * Switches to the alternate screen buffer — the same thing less and vim do.
     * The learner's scrollback is untouched underneath and comes back on exit,
     * which is what makes this feel like entering an application rather than
     * spraying output over whatever they were doing.
     */
    static String enterApplicationScreen() {
        return code("\033[?1049h\033[H\033[2J");
    }

    static String exitApplicationScreen() {
        return code("\033[?1049l");
    }

    static String clearLine() {
        return ENABLED ? "\r\033[2K" : "";
    }

    private static String code(String sequence) {
        return ENABLED ? sequence : "";
    }
}
