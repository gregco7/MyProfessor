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

    subtopic text NOT NULL,

    pass_req smallint NOT NUll CONSTRAINT enforce_pass_lvl CHECK (pass_req BETWEEN 1 AND 10),
    user_pass BOOLEAN NOT NULL
);

-- Learning Sections are under a specific Node for studies before testing

CREATE TABLE kc.learn(
    learn_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    node_id uuid NOT NULL REFERENCES  kc.nodes ON DELETE CASCADE,

    learn_format VARCHAR(32) NOT NULL DEFAULT 'MARKDOWN_LATEX'
);

CREATE TABLE kc.questions (
    question_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    node_id uuid NOT NULL REFERENCES kc.nodes ON DELETE CASCADE,

    body text NOT NULL,
    body_format VARCHAR(32) NOT NULL DEFAULT 'MARKDOWN_LATEX',
    type VARCHAR(32) NOT NULL,

    -- MC Specific
    multi_select BOOLEAN,

    -- Written Specific
    rubric text,
    model_answer text,

    hint1 text NOT NULL,
    hint2 text NOT NULL,

    CONSTRAINT enforce_question_type CHECK ( type in ('MULTIPLE_CHOICE', 'WRITTEN')),

    CONSTRAINT written_need_rubric
        CHECK (type <> 'WRITTEN' OR rubric IS NOT NULL),
    CONSTRAINT mc_needs_select_mode
        CHECK (type <> 'MULTIPLE_CHOICE' OR multi_select IS NOT NULL),
    CONSTRAINT written_has_no_mc_fields
        CHECK (type <> 'WRITTEN' or multi_select IS NULL)
);

-- Sample choices for MC questions

CREATE TABLE kc.choice (
    choice_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id uuid NOT NULL references kc.questions ON DELETE CASCADE,

    body text NOT NUll,
    is_correct BOOLEAN NOT NULL,
    explanation TEXT
);

-- This Table is for establishing a child nodes
-- prerequisite relationship to a parent node.

CREATE TABLE kc.prenodes (
    node_id uuid REFERENCES KC.nodes ON DELETE CASCADE,
    prereq_id uuid REFERENCES KC.nodes ON DELETE CASCADE,
    CHECK (node_id <> prereq_id),
    PRIMARY KEY (node_id,prereq_id)
);

-- FOREIGN KEY INDEXES
CREATE INDEX ON kc.nodes (session_id);
CREATE INDEX ON kc.learn (node_id);
CREATE INDEX ON kc.questions (node_id);
CREATE INDEX on kc.choice (question_id);
CREATE INDEX ON kc.prenodes (prereq_id);