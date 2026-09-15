package dev.gregco7.plan;

/**
 * One option of a multiple-choice question as Claude wrote it.
 *
 * @param explanation why this option is right or wrong, shown once the learner
 *                    has answered; for a wrong option it names the specific
 *                    misunderstanding that would lead someone to pick it
 */
public record PlannedChoice(String body, boolean correct, String explanation) {}
