package dev.gregco7.kc;

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
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "learn", schema = "kc")
public class Learn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "learn_id", updatable = false, nullable = false)
    private UUID learnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Enumerated(EnumType.STRING)
    @Column(name = "learn_format", nullable = false, length = 32)
    private ContentFormat learnFormat = ContentFormat.MARKDOWN_LATEX;

    protected Learn() {}

    public Learn(ContentFormat learnFormat) {
        this.learnFormat = learnFormat;
    }

    public UUID getLearnId() { return learnId; }
    public Node getNode() { return node; }
    public ContentFormat getLearnFormat() { return learnFormat; }

    public void setNode(Node node) { this.node = node; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Learn other)) return false;
        return learnId != null && learnId.equals(other.learnId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
