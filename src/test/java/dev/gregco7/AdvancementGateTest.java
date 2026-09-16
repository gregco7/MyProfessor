package dev.gregco7;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The gate is a database trigger rather than anything in Java, so these go
 * through SQL. Driving them through the service would prove the service checks
 * first — which it does, and which is not the point: the rule has to hold for a
 * writer that never loads a service at all.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AdvancementGateTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void attemptOnALockedLessonIsRejected() {
        UUID session = session();
        UUID prereq = node(session, "prerequisite");
        UUID locked = node(session, "dependent");
        requires(locked, prereq);

        assertThatThrownBy(() -> attempt(locked))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("is locked");

        assertThat(attemptedAt(locked)).isNull();
    }

    @Test
    void passingItsPrerequisiteUnlocksTheLesson() {
        UUID session = session();
        UUID prereq = node(session, "prerequisite");
        UUID dependent = node(session, "dependent");
        requires(dependent, prereq);

        pass(prereq);

        assertThatCode(() -> attempt(dependent)).doesNotThrowAnyException();
        assertThat(attemptedAt(dependent)).isNotNull();
    }

    /** A failed sitting leaves the lesson open; the gate is about prerequisites. */
    @Test
    void anUnlockedLessonCanBeSatAgain() {
        UUID session = session();
        UUID prereq = node(session, "prerequisite");
        UUID dependent = node(session, "dependent");
        requires(dependent, prereq);
        pass(prereq);

        attempt(dependent);

        assertThatCode(() -> attempt(dependent)).doesNotThrowAnyException();
    }

    /** Nothing depends on it, so there is nothing to wait for. */
    @Test
    void aLessonWithNoPrerequisitesIsOpenFromTheStart() {
        UUID starting = node(session(), "starting");

        assertThatCode(() -> attempt(starting)).doesNotThrowAnyException();
    }

    /**
     * A planned session is saved as one graph of unpassed lessons, edges and all.
     * Gating the insert would reject every session at the moment it is created.
     */
    @Test
    void planningASessionIsNotAnAdvancement() {
        UUID session = session();
        UUID prereq = node(session, "prerequisite");

        assertThatCode(() -> requires(node(session, "dependent"), prereq))
                .doesNotThrowAnyException();
    }

    /** The pass flag is gated too, not just the attempt stamp it travels with. */
    @Test
    void aLockedLessonCannotBePassedDirectly() {
        UUID session = session();
        UUID prereq = node(session, "prerequisite");
        UUID locked = node(session, "dependent");
        requires(locked, prereq);

        assertThatThrownBy(() -> pass(locked))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("is locked");
    }

    private UUID session() {
        return jdbc.queryForObject(
                "INSERT INTO app.sessions (topic, lvl) VALUES (?, ?) RETURNING session_id",
                UUID.class, "gating", (short) 1);
    }

    private UUID node(UUID session, String subtopic) {
        return jdbc.queryForObject("""
                INSERT INTO kc.nodes (session_id, subtopic, pass_req, user_pass)
                VALUES (?, ?, ?, false) RETURNING node_id
                """, UUID.class, session, subtopic, (short) 1);
    }

    private void requires(UUID node, UUID prereq) {
        jdbc.update("INSERT INTO kc.prenodes (node_id, prereq_id) VALUES (?, ?)", node, prereq);
    }

    private void attempt(UUID node) {
        jdbc.update("UPDATE kc.nodes SET last_attempted_time = now() WHERE node_id = ?", node);
    }

    private void pass(UUID node) {
        jdbc.update("UPDATE kc.nodes SET user_pass = true WHERE node_id = ?", node);
    }

    private Object attemptedAt(UUID node) {
        return jdbc.queryForMap("SELECT last_attempted_time FROM kc.nodes WHERE node_id = ?", node)
                .get("last_attempted_time");
    }
}
