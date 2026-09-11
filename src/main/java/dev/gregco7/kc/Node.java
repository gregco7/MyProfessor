package dev.gregco7.kc;

import dev.gregco7.session.Session;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "nodes", schema = "kc")
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "node_id", updatable = false, nullable = false)
    private UUID nodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private String subtopic;

    @Column(name = "pass_req", nullable = false)
    private short passReq;

    @Column(name = "user_pass", nullable = false)
    private boolean userPass;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Learn> learnSections = new LinkedHashSet<>();

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Question> questions = new LinkedHashSet<>();

    // kc.prenodes is a pure join table, so it maps as a self-referencing
    // many-to-many rather than an entity of its own.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "prenodes", schema = "kc",
            joinColumns = @JoinColumn(name = "node_id"),
            inverseJoinColumns = @JoinColumn(name = "prereq_id"))
    private Set<Node> prerequisites = new LinkedHashSet<>();

    protected Node() {}

    public Node(String subtopic, short passReq) {
        this.subtopic = subtopic;
        this.passReq = passReq;
        this.userPass = false;
    }

    public UUID getNodeId() { return nodeId; }
    public Session getSession() { return session; }
    public String getSubtopic() { return subtopic; }
    public short getPassRequirement() { return passReq; }
    public boolean isPassed() { return userPass; }
    public Set<Learn> getLearnSections() { return learnSections; }
    public Set<Question> getQuestions() { return questions; }
    public Set<Node> getPrerequisites() { return prerequisites; }

    public void setSession(Session session) { this.session = session; }
    public void setPassed(boolean userPass) { this.userPass = userPass; }

    /** Keeps both sides of the association in sync; the child owns the FK column. */
    public void addLearn(Learn learn) {
        learnSections.add(learn);
        learn.setNode(this);
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.setNode(this);
    }

    public void addPrerequisite(Node prereq) {
        prerequisites.add(prereq);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node other)) return false;
        return nodeId != null && nodeId.equals(other.nodeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
