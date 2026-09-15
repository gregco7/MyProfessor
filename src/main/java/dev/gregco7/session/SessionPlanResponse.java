package dev.gregco7.session;
import java.util.UUID;

public class SessionPlanResponse {
    private int numNodes;
    private String topic;
    private UUID id;
    private String assessment;

    public SessionPlanResponse(int numNodes, String topic, UUID id, String assessment ) {
        this.numNodes = numNodes;
        this.topic = topic;
        this.id = id;
        this.assessment = assessment;
    }

    /** Built from the saved session, so this reports what was planned, not what was asked for. */
    public static SessionPlanResponse of(Session session) {
        return new SessionPlanResponse(
                session.getNodes().size(),
                session.getTopic(),
                session.getSessionId(),
                session.getAssessment());
    }

    public int getNumNodes() { return numNodes; }
    public String getTopic() { return topic; }
    public UUID getId() { return id; }

    /** Why the session is shaped the way it is: what the probe showed about the learner. */
    public String getAssessment() { return assessment; }

    @Override
    public String toString() {
        return "Session with topic "+topic+" created with "+numNodes+" nodes. UUID: "+id;
    }
}
