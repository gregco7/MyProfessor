-- Learning sections were scaffolded with a format but no content. A section is
-- only useful if it carries the material, and the sections under one node are an
-- ordered read-through, so both columns are required.

ALTER TABLE kc.learn
    ADD COLUMN ordinal smallint NOT NULL DEFAULT 1,
    ADD COLUMN body text NOT NULL DEFAULT '';

ALTER TABLE kc.learn ALTER COLUMN ordinal DROP DEFAULT;
ALTER TABLE kc.learn ALTER COLUMN body DROP DEFAULT;

ALTER TABLE kc.learn
    ADD CONSTRAINT enforce_positive_ordinal CHECK (ordinal >= 1),
    ADD CONSTRAINT unique_ordinal_per_node UNIQUE (node_id, ordinal);
