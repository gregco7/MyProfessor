package dev.gregco7.cli;

import dev.gregco7.kc.Choice;
import dev.gregco7.kc.SubmittedAnswer;
import dev.gregco7.kc.Node;
import dev.gregco7.kc.Question;
import dev.gregco7.kc.QuestionType;
import dev.gregco7.plan.Proficiency;
import dev.gregco7.plan.ProbePlanner;
import dev.gregco7.plan.SessionPlanner;
import dev.gregco7.probe.Probe;
import dev.gregco7.probe.ProbeService;
import dev.gregco7.session.Session;
import dev.gregco7.session.SessionService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code /init_learn 'concept' 'proficiency'} — the whole path from a topic to a
 * saved session, sat at the keyboard.
 *
 * <p>Claude writes a diagnostic, the learner answers it here, and the answers
 * are read before a single lesson is chosen. The diagnostic is the point: a
 * course planned without it has to start at the beginning of the topic, which
 * for most people means being taught things they already have.
 */
@Component
class InitLearnCommand implements ShellCommand {

    private static final String[] LETTERS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    private final ProbePlanner probePlanner;
    private final ProbeService probes;
    private final SessionPlanner planner;
    private final SessionService sessions;

    InitLearnCommand(ProbePlanner probePlanner, ProbeService probes,
                     SessionPlanner planner, SessionService sessions) {
        this.probePlanner = probePlanner;
        this.probes = probes;
        this.planner = planner;
        this.sessions = sessions;
    }

    @Override
    public String name() {
        return "/init_learn";
    }

    @Override
    public String usage() {
        return "/init_learn 'concept' 'proficiency'";
    }

    @Override
    public String description() {
        return "sit a diagnostic, then plan a session from where you actually are";
    }

    @Override
    public void run(List<String> args, Term term) {
        if (args.size() != 2) {
            throw new IllegalArgumentException(
                    "expected a concept and a proficiency, got " + args.size() + " argument(s)");
        }
        String concept = args.get(0);
        if (concept.isBlank()) {
            throw new IllegalArgumentException("the concept cannot be empty");
        }
        short lvl = Proficiency.parse(args.get(1));

        term.blank();
        term.heading("  " + concept);
        term.info("  aiming for " + Proficiency.label(lvl) + " (" + lvl + " of 4)");
        term.blank();

        Probe probe = term.whileWaiting(
                "Writing a diagnostic to find where you already stand...",
                () -> probes.create(probePlanner.plan(concept, lvl)));

        List<SubmittedAnswer> answers = sit(probe, term);

        Session session = term.whileWaiting(
                "Reading your answers and planning the session...",
                () -> {
                    Probe sat = probes.recordAnswers(probe.getProbeId(), answers);
                    return sessions.createFromPlan(
                            planner.plan(concept, lvl, resolvedNodeCount(lvl), sat));
                });

        report(session, term);
    }

    /**
     * Puts the diagnostic to the learner one question at a time.
     *
     * <p>Nothing is marked on screen and no correct answer is ever shown: being
     * told the answer partway through would change what the rest of the
     * diagnostic measures. Anything skipped is left unanswered, which the
     * planner reads as evidence rather than treating as a gap in the data.
     */
    private List<SubmittedAnswer> sit(Probe probe, Term term) {
        List<Question> questions = List.copyOf(probe.getQuestions());
        List<SubmittedAnswer> answers = new ArrayList<>();

        term.blank();
        term.heading("  DIAGNOSTIC" + Ansi.RESET + Ansi.DIM + "  " + questions.size()
                + " questions · answer as best you can · press Enter to skip" + Ansi.RESET);
        term.blank();

        for (Question question : questions) {
            term.println("  " + Ansi.BOLD + question.getOrdinal() + "." + Ansi.RESET
                    + " " + question.getBody());

            if (question.getType() == QuestionType.WRITTEN) {
                String written = term.readLine("     " + Ansi.DIM + "your answer ›" + Ansi.RESET + " ");
                if (written == null) {
                    break;
                }
                if (!written.isBlank()) {
                    answers.add(new SubmittedAnswer(question.getQuestionId(), written, null));
                }
            }
            else {
                List<Choice> choices = List.copyOf(question.getChoices());
                for (int i = 0; i < choices.size(); i++) {
                    term.println("       " + Ansi.CYAN + LETTERS[i] + Ansi.RESET
                            + ") " + choices.get(i).getBody());
                }
                boolean multi = Boolean.TRUE.equals(question.getMultiSelect());
                String hint = multi ? "letters, comma separated" : "letter";
                String picked = term.readLine(
                        "     " + Ansi.DIM + hint + " ›" + Ansi.RESET + " ");
                if (picked == null) {
                    break;
                }
                List<UUID> chosen = resolve(picked, choices);
                if (!chosen.isEmpty()) {
                    answers.add(new SubmittedAnswer(question.getQuestionId(), null, chosen));
                }
            }
            term.blank();
        }
        return answers;
    }

    /** Maps typed letters back to choice ids, ignoring anything that is not one. */
    private List<UUID> resolve(String typed, List<Choice> choices) {
        List<UUID> chosen = new ArrayList<>();
        for (String part : typed.toLowerCase().split("[,\\s]+")) {
            for (int i = 0; i < choices.size() && i < LETTERS.length; i++) {
                if (LETTERS[i].equals(part)) {
                    chosen.add(choices.get(i).getChoiceId());
                }
            }
        }
        return chosen;
    }

    private void report(Session session, Term term) {
        term.blank();
        term.good("  Session ready.");
        term.blank();

        if (session.getAssessment() != null && !session.getAssessment().isBlank()) {
            term.heading("  WHERE YOU STAND");
            for (String line : wrap(session.getAssessment(), 74)) {
                term.println("    " + line);
            }
            term.blank();
        }

        term.heading("  LESSONS");
        int index = 1;
        for (Node node : session.getNodes()) {
            term.println("    " + Ansi.CYAN + index++ + "." + Ansi.RESET + " " + node.getSubtopic()
                    + Ansi.DIM + "  (" + node.getLearnSections().size() + " sections, "
                    + node.getQuestions().size() + " questions, pass "
                    + node.getPassRequirement() + ")" + Ansi.RESET);
        }
        term.blank();
        term.info("  " + session.getSessionId());
    }

    /** Higher goal proficiency means a finer breakdown of the same topic. */
    private static int resolvedNodeCount(short lvl) {
        return switch (lvl) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            default -> 9;
        };
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
