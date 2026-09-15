package dev.gregco7.plan;

/** How app.sessions.lvl reads to Claude. */
final class Proficiency {

    private Proficiency() {}

    static String brief(short lvl) {
        return switch (lvl) {
            case 1 -> "1 of 4 — aware: can follow a conversation on the topic and recognise its ideas";
            case 2 -> "2 of 4 — working familiarity: can apply the standard cases with a reference open";
            case 3 -> "3 of 4 — competent: can apply the ideas to unfamiliar problems unaided";
            default -> "4 of 4 — fluent: can reason about edge cases and teach the topic to someone else";
        };
    }
}
