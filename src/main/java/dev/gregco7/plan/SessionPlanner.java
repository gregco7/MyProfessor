package dev.gregco7.plan;

import dev.gregco7.kc.ContentFormat;
import dev.gregco7.kc.Learn;
import dev.gregco7.kc.Node;
import dev.gregco7.kc.Question;
import dev.gregco7.probe.Probe;
import dev.gregco7.session.Session;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Turns a topic — and, when there is one, a sat probe — into a full, unsaved
 * {@link Session}: lessons, the material to study under each, and the questions
 * that decide whether the learner has it.
 *
 * <p>Planning runs in two passes. One request reads the probe and produces the
 * outline: the reading of the learner, the lesson titles, and the prerequisite
 * edges between them. Then one request per lesson writes that lesson's material
 * and questions. Asking for the whole session in a single response would force
 * Claude to ration tokens across every lesson at once, which is the pressure
 * that turns meticulous material into a bullet list; a request per lesson gives
 * each one the full output budget. The detail requests are independent, so they
 * run concurrently.
 *
 * <p>Nothing here touches the database. The returned graph is transient and the
 * caller persists it in a short transaction, because these requests take minutes
 * and a connection should not be held open across them.
 */
@Service
public class SessionPlanner {

    // The outline is a handful of titles; the material under one lesson is the
    // long part, and truncation there is what the two-pass split exists to avoid.
    // Both ceilings also have to cover thinking, which is drawn from the same
    // budget — size one for the JSON alone and a long think returns nothing at
    // all. Headroom that goes unused is not billed.
    private static final int OUTLINE_MAX_TOKENS = 32_000;
    private static final int DETAIL_MAX_TOKENS = 48_000;

    private static final String OUTLINE_SYSTEM = """
            You are a curriculum architect. Given a topic, a target proficiency, and a \
            diagnostic the learner has already sat, you decompose the topic into a \
            dependency-ordered sequence of lessons that carries this learner from where \
            they actually are to that proficiency.

            Read the diagnostic first, and write learnerAssessment before anything else. \
            The transcript gives each question, what a full answer was, and what the \
            learner gave. Read the shape of it, not the score:
            - A wrong choice is evidence of a specific belief. Say which belief, where the \
              explanation of that choice tells you.
            - A right answer at a level above a wrong one usually means the wrong one was a \
              gap in a particular area, not a ceiling. Say which area.
            - Answers that are right but thin, or right for the wrong reason, mark something \
              shaky rather than held. Treat shaky as not yet held.
            - Questions left blank are evidence too, especially a run of them at the end.
            - If no diagnostic was sat, say so in learnerAssessment and plan from the start \
              of the topic.

            Then choose the lessons:
            - Do not write a lesson for something the diagnostic shows the learner already \
              holds solidly. Starting below the edge of their understanding is the failure \
              this diagnostic exists to prevent.
            - Do write one where the diagnostic shows a partial or confused grasp; \
              re-teaching a shaky foundation is not repetition.
            - Begin at the first thing they do not have, and carry on to the target \
              proficiency.
            - Produce exactly the number of lessons requested.
            - Each lesson covers one idea that can be taught and tested on its own. No \
              filler lessons such as "Introduction", "Overview", "Conclusion" or "Next \
              steps"; every lesson must teach something testable.
            - Order the lessons so that a lesson only ever depends on lessons before it. \
              List those dependencies by their zero-based position in your own list, and \
              only direct ones: if A is needed for B and B for C, C depends on B, not on A. \
              A lesson with no dependencies gets an empty list. A lesson resting on \
              something the diagnostic shows the learner already holds also gets an empty \
              list — that knowledge is theirs already, not a lesson in this session.
            - The objective states what the learner can do after the lesson, in the \
              concrete terms you would use to write a test question — not "understand X".
            - passReq is how many questions out of ten must be answered well to count the \
              lesson as passed. Use 6-7 for foundations where partial recall is workable, \
              8-10 for anything the learner must have exactly right before moving on, and \
              lean higher where the diagnostic showed a misconception in that area, since \
              a half-corrected misconception is worse than an absent one.
            """;

    private static final String DETAIL_SYSTEM = """
            You are a subject-matter tutor writing one lesson of a larger course, for one \
            named learner whose standing you are told. You write the study material and \
            then the questions that test it.

            Material:
            - Write 2 to 5 sections. Each section is the full teaching text for one part of \
              the lesson, in Markdown, with LaTeX between $ ... $ for inline maths and \
              $$ ... $$ for displayed maths. Sections are read in the order you give them.
            - Teach the material; do not summarise it. Define every term you use, derive \
              results rather than asserting them, and show the intermediate steps of any \
              calculation. Where an idea is commonly misunderstood, say what the wrong \
              model is and why it fails. Ground the lesson in at least one worked example \
              carried through to its conclusion.
            - Pitch it at the learner described. Where the assessment says they already \
              hold something, use it without re-teaching it. Where it names a specific \
              misconception this lesson touches, address that misconception directly rather \
              than teaching around it.
            - Assume only what the learner already holds and the prerequisite lessons \
              listed below. Do not lean on later lessons they have not reached.
            - Start each section with a Markdown heading. Do not address the reader with \
              course-scaffolding chatter ("in this section we will...", "as we saw earlier").

            Questions:
            - Write 10 questions that test the lesson's objective, not recall of its \
              wording. Aim for roughly two-thirds multiple choice and one-third written, \
              and order them from the most basic check to the hardest application.
            - hint1 nudges the learner toward the right approach. hint2 is more direct but \
              still stops short of giving the answer. Neither may state the answer.
            - MULTIPLE_CHOICE: give 4 or 5 choices and set multiSelect to true only when \
              more than one is correct. Every choice needs an explanation, including the \
              correct ones — a wrong choice's explanation names the specific \
              misunderstanding that would lead someone to pick it. Do not make the correct \
              choice the longest or most qualified one. Leave rubric and modelAnswer null.
            - WRITTEN: give a rubric that lists the specific points an answer must make to \
              earn full marks, and a modelAnswer that earns full marks under it. Leave \
              multiSelect and choices null.
            """;

    private static final String NO_DIAGNOSTIC =
            "No diagnostic was sat. Nothing is known about this learner beyond the topic "
                    + "and the proficiency they are aiming at.";

    private final PlanningChat chat;

    SessionPlanner(PlanningChat chat) {
        this.chat = chat;
    }

    /**
     * Plans a session end to end and returns it unsaved.
     *
     * @param topic    what to teach
     * @param lvl      goal proficiency, 1-4
     * @param numNodes how many lessons to produce
     * @param probe    the diagnostic the learner sat, with their answers already
     *                 recorded on its questions, or null to plan from the topic
     *                 alone — which means starting at the beginning of it
     */
    public Session plan(String topic, short lvl, int numNodes, @Nullable Probe probe) {
        String diagnostic = probe != null ? ProbeTranscript.render(probe) : NO_DIAGNOSTIC;

        CurriculumOutline outline = requestOutline(topic, lvl, numNodes, diagnostic);
        List<CurriculumOutline.OutlineNode> outlineNodes = usableNodes(outline, numNodes);
        String assessment = outline.learnerAssessment();

        List<NodeDetail> details = requestDetails(topic, lvl, assessment, outlineNodes);

        Session session = new Session(topic, lvl);
        session.recordDiagnosis(probe, assessment);

        List<Node> nodes = new ArrayList<>(outlineNodes.size());
        for (int i = 0; i < outlineNodes.size(); i++) {
            Node node = buildNode(outlineNodes.get(i), details.get(i));
            session.addNode(node);
            nodes.add(node);
        }

        // Wired after every node exists, since an edge points at a sibling.
        for (int i = 0; i < outlineNodes.size(); i++) {
            for (int prereq : prerequisitesOf(outlineNodes.get(i), i)) {
                nodes.get(i).addPrerequisite(nodes.get(prereq));
            }
        }

        return session;
    }

    private CurriculumOutline requestOutline(
            String topic, short lvl, int numNodes, String diagnostic) {

        CurriculumOutline outline = chat.ask(
                OUTLINE_SYSTEM,
                """
                Topic: %s
                Target proficiency: %s
                Number of lessons: %d

                Diagnostic the learner sat:
                %s
                """.formatted(topic, Proficiency.brief(lvl), numNodes, diagnostic),
                OUTLINE_MAX_TOKENS,
                CurriculumOutline.class,
                "plan a session for the topic: " + topic);

        if (outline.nodes() == null || outline.nodes().isEmpty()) {
            throw new PlanGenerationException(
                    "The outline for '" + topic + "' contained no lessons");
        }
        return outline;
    }

    /**
     * One request per lesson, run concurrently. Each response is long enough that
     * doing these in sequence would make the endpoint's latency the sum of all of
     * them; they share no state, so nothing is gained by serialising.
     */
    private List<NodeDetail> requestDetails(
            String topic, short lvl, String assessment,
            List<CurriculumOutline.OutlineNode> outlineNodes) {

        List<NodeDetail> details = new ArrayList<>(outlineNodes.size());
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<NodeDetail>> pending = new ArrayList<>(outlineNodes.size());
            for (int i = 0; i < outlineNodes.size(); i++) {
                int index = i;
                pending.add(workers.submit(
                        () -> requestDetail(topic, lvl, assessment, outlineNodes, index)));
            }
            for (int i = 0; i < pending.size(); i++) {
                details.add(awaitDetail(pending.get(i), outlineNodes.get(i).subtopic()));
            }
        }
        return details;
    }

    private NodeDetail awaitDetail(Future<NodeDetail> pending, String subtopic) {
        try {
            return pending.get();
        }
        catch (ExecutionException ex) {
            if (ex.getCause() instanceof PlanGenerationException planFailure) {
                throw planFailure;
            }
            throw new PlanGenerationException(
                    "Claude could not write the lesson: " + subtopic, ex.getCause());
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PlanGenerationException(
                    "Interrupted while writing the lesson: " + subtopic, ex);
        }
    }

    private NodeDetail requestDetail(
            String topic, short lvl, String assessment,
            List<CurriculumOutline.OutlineNode> outlineNodes, int index) {

        CurriculumOutline.OutlineNode outlineNode = outlineNodes.get(index);

        StringBuilder prerequisites = new StringBuilder();
        for (int prereq : prerequisitesOf(outlineNode, index)) {
            prerequisites.append("- ")
                    .append(outlineNodes.get(prereq).subtopic())
                    .append(": ")
                    .append(outlineNodes.get(prereq).objective())
                    .append('\n');
        }
        if (prerequisites.isEmpty()) {
            prerequisites.append("- none from this session; this is a starting lesson\n");
        }

        return chat.ask(
                DETAIL_SYSTEM,
                """
                Course topic: %s
                Target proficiency for the course: %s

                Where this learner stands:
                %s

                Lesson %d of %d: %s
                Objective: %s

                Lessons of this session they have already passed:
                %s
                """.formatted(
                        topic,
                        Proficiency.brief(lvl),
                        assessment != null && !assessment.isBlank() ? assessment : NO_DIAGNOSTIC,
                        index + 1,
                        outlineNodes.size(),
                        outlineNode.subtopic(),
                        outlineNode.objective(),
                        prerequisites),
                DETAIL_MAX_TOKENS,
                NodeDetail.class,
                "write the lesson: " + outlineNode.subtopic());
    }

    private Node buildNode(CurriculumOutline.OutlineNode outlineNode, NodeDetail detail) {
        List<Learn> sections = new ArrayList<>();
        short sectionOrdinal = 1;
        for (NodeDetail.LearnSection section : nullSafe(detail.learnSections())) {
            if (section == null || section.body() == null || section.body().isBlank()) {
                continue;
            }
            sections.add(new Learn(sectionOrdinal++, section.body(), ContentFormat.MARKDOWN_LATEX));
        }
        if (sections.isEmpty()) {
            throw new PlanGenerationException(
                    "No study material was written for the lesson: " + outlineNode.subtopic());
        }

        List<Question> questions = new ArrayList<>();
        short questionOrdinal = 1;
        for (PlannedQuestion planned : nullSafe(detail.questions())) {
            if (planned == null) {
                continue;
            }
            Question question = planned.toQuestion(questionOrdinal);
            if (question != null) {
                questions.add(question);
                questionOrdinal++;
            }
        }
        if (questions.isEmpty()) {
            throw new PlanGenerationException(
                    "No questions were written for the lesson: " + outlineNode.subtopic());
        }

        Node node = new Node(
                outlineNode.subtopic(), passRequirement(outlineNode.passReq(), questions.size()));
        sections.forEach(node::addLearn);
        questions.forEach(node::addQuestion);
        return node;
    }

    /** Keeps only the lessons that can stand as rows. */
    private static List<CurriculumOutline.OutlineNode> usableNodes(
            CurriculumOutline outline, int numNodes) {

        List<CurriculumOutline.OutlineNode> usable = outline.nodes().stream()
                .filter(node -> node != null && node.subtopic() != null && !node.subtopic().isBlank())
                .limit(numNodes)
                .toList();

        if (usable.isEmpty()) {
            throw new PlanGenerationException("The outline contained no usable lessons");
        }
        return usable;
    }

    /**
     * Drops prerequisite edges that point outside the outline or forward into a
     * lesson the learner has not reached — either would make the course unwalkable.
     */
    private static List<Integer> prerequisitesOf(
            CurriculumOutline.OutlineNode node, int ownIndex) {

        return nullSafe(node.prerequisiteIndexes()).stream()
                .filter(prereq -> prereq != null && prereq >= 0 && prereq < ownIndex)
                .distinct()
                .toList();
    }

    /**
     * kc.nodes.pass_req is constrained to 1-10, and a requirement above the number
     * of questions that survived would leave the lesson impossible to pass.
     */
    private static short passRequirement(short passReq, int questionCount) {
        return (short) Math.clamp(passReq, 1, Math.min(10, questionCount));
    }

    private static <T> List<T> nullSafe(List<T> values) {
        return values != null ? values : List.of();
    }
}
