package dev.gregco7.session;

import jakarta.persistence.*;
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

    protected Session() {}

    public Session(String topic, short lvl) {
        this.topic = topic;
        this.lvl = lvl;
    }

    public UUID getSessionId () {return sessionId;}
    public String getTopic () {return topic;}
    public short getGoalProficiency () {return lvl;}
}
