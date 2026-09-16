-- Prerequisite gating used to live only in the dashboard, which derived each
-- lesson's availability from the edges it was sent and declined to open the ones
-- that were locked. Nothing behind it agreed: POSTing an attempt at a locked
-- lesson recorded the attempt and could pass it, so the ordering the session was
-- planned around held for as long as the client chose to honour it.
--
-- The rule belongs where it cannot be skipped. A CHECK constraint cannot express
-- it — the answer depends on other rows, and CHECK may not read them — so it is
-- a trigger, which applies to every writer including psql.

CREATE FUNCTION kc.reject_locked_attempt() RETURNS trigger AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM kc.prenodes pn
        JOIN kc.nodes prereq ON prereq.node_id = pn.prereq_id
        WHERE pn.node_id = NEW.node_id
          AND NOT prereq.user_pass
    ) THEN
        RAISE EXCEPTION 'lesson % is locked: it has prerequisites that have not been passed',
            NEW.node_id
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Fires on every UPDATE rather than on UPDATE OF last_attempted_time, because
-- that form fires on whatever columns the statement names — and Hibernate names
-- them all on every write, attempt or not. What makes an update an advancement
-- is therefore decided here, by comparing the rows: the attempt stamp moving, or
-- the pass flag being raised. Updates that touch neither are left alone, and
-- INSERT is not covered at all, since a planned session arrives as one graph of
-- unpassed lessons and gating it on the way in would reject every session.
CREATE TRIGGER gate_advancement
    BEFORE UPDATE ON kc.nodes
    FOR EACH ROW
    WHEN (NEW.last_attempted_time IS DISTINCT FROM OLD.last_attempted_time
          OR (NEW.user_pass AND NOT OLD.user_pass))
    EXECUTE FUNCTION kc.reject_locked_attempt();
