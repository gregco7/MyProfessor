package dev.gregco7.plan;

import java.util.Locale;

/** How app.sessions.lvl reads — to Claude, and to whoever typed it at the prompt. */
public final class Proficiency {

    private Proficiency() {}

    /**
     * Accepts the number the column stores or the word it stands for, because
     * "competent" is what someone means and "3" is what the database wants.
     *
     * @throws IllegalArgumentException if it is neither, with the accepted forms
     */
    public static short parse(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "1", "aware" -> 1;
            case "2", "familiar", "working" -> 2;
            case "3", "competent" -> 3;
            case "4", "fluent" -> 4;
            default -> throw new IllegalArgumentException(
                    "proficiency must be 1-4, or one of: aware, familiar, competent, fluent "
                            + "(got '" + raw + "')");
        };
    }

    /** The one-word name, for reporting back what was understood. */
    public static String label(short lvl) {
        return switch (lvl) {
            case 1 -> "aware";
            case 2 -> "familiar";
            case 3 -> "competent";
            default -> "fluent";
        };
    }

    static String brief(short lvl) {
        return switch (lvl) {
            case 1 -> "1 of 4 — aware: can follow a conversation on the topic and recognise its ideas";
            case 2 -> "2 of 4 — working familiarity: can apply the standard cases with a reference open";
            case 3 -> "3 of 4 — competent: can apply the ideas to unfamiliar problems unaided";
            default -> "4 of 4 — fluent: can reason about edge cases and teach the topic to someone else";
        };
    }
}
