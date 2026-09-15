package dev.gregco7.kc;

import java.util.List;
import java.util.UUID;

/**
 * What a learner gave for one question. Which field carries the answer follows
 * the question's own shape; the other is ignored.
 *
 * <p>The same record serves a probe question and a lesson question, because they
 * are the same rows in the same table asked at different moments.
 *
 * @param written   the learner's prose, for a WRITTEN question
 * @param choiceIds the options they picked, for a MULTIPLE_CHOICE question
 */
public record SubmittedAnswer(UUID questionId, String written, List<UUID> choiceIds) {}
