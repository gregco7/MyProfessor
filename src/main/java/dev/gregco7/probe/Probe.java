package dev.gregco7.probe;

import dev.gregco7.kc.Question;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The diagnostic a learner sits before a session is planned for them.
 *
 * <p>A probe is a short spread of questions over the whole topic, from the
 * foundations to the far end of the target proficiency. Nothing about it is
 * taught: its only job is to find where the learner's understanding runs out, so
 * the session can be built from that point outward rather than from the start of
 * a topic they may already be halfway through.
 *
 * <p>Its questions are ordinary {@link Question} rows. A probe question and a
 * lesson question are the same object asked at a different moment.
 */
@Entity
@Table(name = "probes", schema = "app")
public class Probe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "probe_id", updatable = false, nullable = false)
    private UUID probeId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private short lvl;

    @OneToMany(mappedBy = "probe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinal")
    private Set<Question> questions = new LinkedHashSet<>();

    protected Probe() {}

    public Probe(String topic, short lvl) {
        this.topic = topic;
        this.lvl = lvl;
    }

    public UUID getProbeId() { return probeId; }
    public String getTopic() { return topic; }
    public short getGoalProficiency() { return lvl; }
    public Set<Question> getQuestions() { return questions; }

    /** Keeps both sides in sync; Question owns the probe_id column. */
    public void addQuestion(Question question) {
        questions.add(question);
        question.setProbe(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Probe other)) return false;
        return probeId != null && probeId.equals(other.probeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
