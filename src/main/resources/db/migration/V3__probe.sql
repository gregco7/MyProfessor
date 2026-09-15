-- A probe is the diagnostic the learner sits before a session is planned. Its
-- questions exist to locate the edge of what they already know, so the session
-- can start there instead of at the beginning of the topic.

CREATE TABLE app.probes (
    probe_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,

    topic text NOT NULL,
    lvl smallint NOT NULL CONSTRAINT enforce_probe_lvls CHECK (lvl BETWEEN 1 AND 4)
);

-- Probe questions are the same thing as node questions in every respect the
-- learner can see, so they are the same rows. What differs is who owns them:
-- a question hangs off exactly one of a node or a probe, never both or neither.

ALTER TABLE kc.questions
    ALTER COLUMN node_id DROP NOT NULL,
    ADD COLUMN probe_id uuid REFERENCES app.probes ON DELETE CASCADE,
    ADD CONSTRAINT enforce_single_owner CHECK (num_nonnulls(node_id, probe_id) = 1);

-- Questions are asked in a deliberate order: easiest first within a lesson, and
-- across a probe an ascending sweep whose job is to bracket the learner between
-- the last question they get right and the first they get wrong. A set of rows
-- has no order of its own, so the order is stored.

ALTER TABLE kc.questions ADD COLUMN ordinal smallint NOT NULL DEFAULT 1;
ALTER TABLE kc.questions ALTER COLUMN ordinal DROP DEFAULT;
ALTER TABLE kc.questions
    ADD CONSTRAINT enforce_positive_question_ordinal CHECK (ordinal >= 1);

CREATE UNIQUE INDEX unique_question_ordinal_per_node
    ON kc.questions (node_id, ordinal) WHERE node_id IS NOT NULL;
CREATE UNIQUE INDEX unique_question_ordinal_per_probe
    ON kc.questions (probe_id, ordinal) WHERE probe_id IS NOT NULL;

CREATE INDEX ON kc.questions (probe_id);

-- The session records the probe it was planned from and the reading of the
-- learner that came out of it, so the shape of the tree can be explained later.

ALTER TABLE app.sessions
    ADD COLUMN probe_id uuid REFERENCES app.probes ON DELETE SET NULL,
    ADD COLUMN assessment text;

CREATE INDEX ON app.sessions (probe_id);
