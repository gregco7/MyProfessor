-- When a session was planned, and when each lesson was last sat.

ALTER TABLE app.sessions
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();

-- Null until the learner first sits the lesson's questions, then stamped on
-- every attempt whether it passed or not. It answers "when did I last work on
-- this", which user_pass cannot: a lesson failed three times running and a
-- lesson never opened both read as not passed, and only this tells them apart.
ALTER TABLE kc.nodes
    ADD COLUMN last_attempted_time timestamptz NULL;

-- The dashboard's default ordering is newest first.
CREATE INDEX ON app.sessions (created_at DESC);
