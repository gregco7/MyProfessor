package dev.gregco7.plan;

import java.util.List;

/**
 * The diagnostic Claude writes to find out where a learner already stands on a
 * topic. Nothing here teaches; every question exists to bracket the learner
 * between what they clearly have and what they clearly do not.
 *
 * @param coverage one line per question saying which part of the topic it probes
 *                 and at what depth, so the spread can be checked at a glance
 */
public record ProbePlan(List<String> coverage, List<PlannedQuestion> questions) {}
