package dev.gregco7.plan;

import java.util.List;

/**
 * Second pass of planning: everything that hangs off one lesson. Requested per
 * node so each response has room for material written at length rather than a
 * summary squeezed in beside every other lesson in the session.
 */
public record NodeDetail(
        List<LearnSection> learnSections,
        List<PlannedQuestion> questions) {

    /** @param body the teaching material itself, Markdown with LaTeX for any maths */
    public record LearnSection(String body) {}
}
