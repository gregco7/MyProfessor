package dev.gregco7.plan;

import java.util.List;

/**
 * First pass of planning: the shape of the session only. Claude reads the sat
 * probe, says what it shows about the learner, then names the lessons and the
 * dependencies between them — but writes no material yet, so the whole skeleton
 * comes back in one small, cheap response that the detail pass fans out over.
 *
 * @param learnerAssessment where the learner stands, drawn from the probe: what
 *                          they already hold, what is shaky, and the first thing
 *                          they do not have. Written before the lessons because
 *                          it is what the lessons are chosen to fit, and kept on
 *                          the session so the shape of the tree can be explained.
 * @param nodes             ordered so that a lesson's prerequisites always precede it
 */
public record CurriculumOutline(String learnerAssessment, List<OutlineNode> nodes) {

    /**
     * @param subtopic             the lesson title
     * @param objective            what the learner can do once this lesson is passed;
     *                             carried into the detail pass as the target to write against
     * @param passReq              questions out of ten that must be answered well to pass, 1-10
     * @param prerequisiteIndexes  zero-based positions in {@code nodes} of the lessons
     *                             that must be passed first; always smaller than this
     *                             lesson's own position
     */
    public record OutlineNode(
            String subtopic,
            String objective,
            short passReq,
            List<Integer> prerequisiteIndexes) {}
}
