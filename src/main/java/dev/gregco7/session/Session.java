package dev.gregco7.session;

import dev.gregco7.kc.Node;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "sessions", schema = "app")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id",updatable = false,nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private short lvl;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Node> nodes = new LinkedHashSet<>();

    protected Session() {}

    public Session(String topic, short lvl) {
        this.topic = topic;
        this.lvl = lvl;
    }

    public UUID getSessionId () {return sessionId;}
    public String getTopic () {return topic;}
    public short getGoalProficiency () {return lvl;}
    public Set<Node> getNodes () {return nodes;}

    /** Keeps both sides in sync; Node owns the session_id column. */
    public void addNode(Node node) {
        nodes.add(node);
        node.setSession(this);
    }

    // Identity is the persistent key only. A Session that has not been saved yet
    // has a null id and is equal to nothing but itself, which keeps a transient
    // instance from colliding with others once it lands in a collection.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Session other)) return false;
        return sessionId != null && sessionId.equals(other.sessionId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
