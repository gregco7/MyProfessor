package dev.gregco7.kc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "choice", schema = "kc")
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "choice_id", updatable = false, nullable = false)
    private UUID choiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String body;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column
    private String explanation;

    protected Choice() {}

    public Choice(String body, boolean correct, String explanation) {
        this.body = body;
        this.correct = correct;
        this.explanation = explanation;
    }

    public UUID getChoiceId() { return choiceId; }
    public Question getQuestion() { return question; }
    public String getBody() { return body; }
    public boolean isCorrect() { return correct; }
    public String getExplanation() { return explanation; }

    public void setQuestion(Question question) { this.question = question; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Choice other)) return false;
        return choiceId != null && choiceId.equals(other.choiceId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
