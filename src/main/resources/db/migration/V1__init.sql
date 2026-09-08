CREATE schema kc; -- Knowledge Content
CREATE schema app; -- For Data at the Highest Abstraction

CREATE TABLE app.sessions (
    session_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,

    topic text NOT NULL,
    lvl smallint NOT NULL CONSTRAINT enforce_standard_lvls CHECK (lvl BETWEEN 1 AND 4)
);

CREATE TABLE kc.nodes (
    node_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id uuid NOT NULL REFERENCES APP.sessions ON DELETE CASCADE,

    subtopic text NOT NULL
);

CREATE TABLE kc.questions (
    question_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    node_id uuid NOT NULL REFERENCES kc.nodes ON DELETE CASCADE
);

-- This Table is for establishing a child nodes
-- prerequisite relationship to a parent node.

CREATE TABLE kc.prenodes (
    node_id uuid REFERENCES KC.nodes ON DELETE CASCADE,
    prereq_id uuid REFERENCES KC.nodes ON DELETE CASCADE,
    CHECK (node_id <> prereq_id),
    PRIMARY KEY (node_id,prereq_id)
);