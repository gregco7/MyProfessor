package dev.gregco7.kc;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "questions", schema = "kc")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "question_id", updatable = false, nullable = false)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_format", nullable = false, length = 32)
    private ContentFormat bodyFormat = ContentFormat.MARKDOWN_LATEX;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(name = "hint1", nullable = false)
    private String hint1;

    @Column(name = "hint2", nullable = false)
    private String hint2;

    // Multiple-choice only. Null for written questions, per written_has_no_mc_fields.
    @Column(name = "multi_select")
    private Boolean multiSelect;

    // Written only. rubric is required when type is WRITTEN, per written_need_rubric.
    @Column
    private String rubric;

    @Column(name = "model_answer")
    private String modelAnswer;

    @Column(name = "answer_written")
    private String answerWritten;

    @Column(name = "grade_written")
    private Short gradeWritten;

    @Column(name = "answer_mc")
    private String answerMc;

    @Column(name = "grade_mc")
    private Boolean gradeMc;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Choice> choices = new LinkedHashSet<>();

    protected Question() {}

    private Question(String body, QuestionType type, String hint1, String hint2) {
        this.body = body;
        this.type = type;
        this.hint1 = hint1;
        this.hint2 = hint2;
    }

    /**
     * The kc.questions check constraints make the two question shapes mutually
     * exclusive, so construction goes through a factory per shape rather than one
     * constructor that can build a row the database will reject.
     */
    public static Question multipleChoice(
            String body, String hint1, String hint2, boolean multiSelect) {
        Question q = new Question(body, QuestionType.MULTIPLE_CHOICE, hint1, hint2);
        q.multiSelect = multiSelect;
        return q;
    }

    public static Question written(
            String body, String hint1, String hint2, String rubric, String modelAnswer) {
        Question q = new Question(body, QuestionType.WRITTEN, hint1, hint2);
        q.rubric = rubric;
        q.modelAnswer = modelAnswer;
        return q;
    }

    public UUID getQuestionId() { return questionId; }
    public Node getNode() { return node; }
    public String getBody() { return body; }
    public ContentFormat getBodyFormat() { return bodyFormat; }
    public QuestionType getType() { return type; }
    public String getHint1() { return hint1; }
    public String getHint2() { return hint2; }
    public Boolean getMultiSelect() { return multiSelect; }
    public String getRubric() { return rubric; }
    public String getModelAnswer() { return modelAnswer; }
    public String getAnswerWritten() { return answerWritten; }
    public Short getGradeWritten() { return gradeWritten; }
    public String getAnswerMc() { return answerMc; }
    public Boolean getGradeMc() { return gradeMc; }
    public Set<Choice> getChoices() { return choices; }

    public void setNode(Node node) { this.node = node; }

    public void addChoice(Choice choice) {
        choices.add(choice);
        choice.setQuestion(this);
    }

    public void submitWrittenAnswer(String answer, Short grade) {
        this.answerWritten = answer;
        this.gradeWritten = grade;
    }

    public void submitMcAnswer(String answer, Boolean grade) {
        this.answerMc = answer;
        this.gradeMc = grade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question other)) return false;
        return questionId != null && questionId.equals(other.questionId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
