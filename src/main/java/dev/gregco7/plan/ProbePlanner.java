package dev.gregco7.plan;

import dev.gregco7.kc.Question;
import dev.gregco7.probe.Probe;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes the diagnostic that a session is later planned from.
 *
 * <p>A course built from a topic alone has to start at the beginning of that
 * topic, which means teaching most learners things they already have. The probe
 * exists to avoid that: a short ascending sweep of questions whose answers say
 * where this particular learner's understanding runs out, so the session can be
 * built outward from that edge.
 */
@Service
public class ProbePlanner {

    /**
     * Thinking is drawn from the same ceiling as the answer, so a budget sized
     * for the JSON alone lets a long think starve the response — the model spends
     * the lot reasoning and returns nothing. Unused headroom is not billed, so
     * this is deliberately far larger than the diagnostic needs.
     */
    private static final int PROBE_MAX_TOKENS = 32_000;

    /** Enough to bracket a learner without turning the diagnostic into the course. */
    private static final int PROBE_QUESTIONS = 8;

    private static final String PROBE_SYSTEM = """
            You are an assessor. Before anyone is taught, you work out what they already \
            know, so that a course can be built outward from the edge of their \
            understanding instead of from the beginning of a topic they may be halfway \
            through. You write the diagnostic; you do not teach.

            Rules:
            - Write exactly the number of questions requested, ordered from the most \
              elementary thing anyone studying this topic would need up to the far end of \
              the stated target proficiency.
            - Space them so consecutive questions are a real step apart. The point is to \
              bracket the learner between the last question they answer well and the first \
              they do not; questions bunched at one difficulty cannot do that.
            - Spread them across the distinct parts of the topic rather than drilling the \
              same part at rising difficulty, so a gap in one area is visible as a gap in \
              that area and not as a general ceiling.
            - Every question must discriminate. One that almost everyone answers the same \
              way tells you nothing. Prefer questions where a particular wrong answer is \
              evidence of a particular missing piece.
            - Teach nothing. Do not explain, do not define the term the question is \
              testing, and do not hint inside the question body.
            - MULTIPLE_CHOICE: 4 or 5 choices. Make the wrong choices the answers a learner \
              holding a specific partial understanding would actually give — not filler — \
              and use each explanation to say what picking it implies about where they \
              are. Set multiSelect true only when more than one choice is correct. Do not \
              make the correct choice the longest or most qualified one. Leave rubric and \
              modelAnswer null.
            - WRITTEN: use these where a learner's own words expose depth that picking an \
              option cannot, and make roughly a quarter of the questions written. The \
              rubric lists the specific points a full answer makes; modelAnswer earns full \
              marks under it. Leave multiSelect and choices null.
            - hint1 and hint2 are recorded but are never shown while the diagnostic is \
              being sat, since a hint would measure the hint rather than the learner. \
              Write them as you would for a lesson.
            - coverage carries one line per question, in the same order, naming the part of \
              the topic that question probes and the depth it probes it at.
            """;

    private final PlanningChat chat;

    ProbePlanner(PlanningChat chat) {
        this.chat = chat;
    }

    /**
     * Writes a diagnostic for the topic and returns it unsaved.
     *
     * @param lvl the proficiency the learner is aiming at, 1-4; it sets the top of
     *            the range the questions sweep, not the bottom
     */
    public Probe plan(String topic, short lvl) {
        ProbePlan plan = chat.ask(
                PROBE_SYSTEM,
                """
                Topic: %s
                Target proficiency the learner is aiming at: %s
                Number of questions: %d
                """.formatted(topic, Proficiency.brief(lvl), PROBE_QUESTIONS),
                PROBE_MAX_TOKENS,
                ProbePlan.class,
                "write a diagnostic for the topic: " + topic);

        Probe probe = new Probe(topic, lvl);
        List<Question> questions = new ArrayList<>();

        short ordinal = 1;
        for (PlannedQuestion planned : plan.questions() != null ? plan.questions() : List.<PlannedQuestion>of()) {
            if (planned == null) {
                continue;
            }
            Question question = planned.toQuestion(ordinal);
            if (question != null) {
                questions.add(question);
                ordinal++;
            }
        }

        if (questions.isEmpty()) {
            throw new PlanGenerationException(
                    "The diagnostic for '" + topic + "' contained no usable questions");
        }
        questions.forEach(probe::addQuestion);
        return probe;
    }
}
