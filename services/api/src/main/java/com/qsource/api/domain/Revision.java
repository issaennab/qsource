package com.qsource.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revisions", indexes = {
        @Index(name = "idx_rev_target", columnList = "targetType,targetId")
})
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private int version;

    /** Full JSON snapshot of the work item at this revision. */
    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String contentJson;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private String actorName;

    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevisionSource source;

    private UUID parentRevisionId;

    @Column(nullable = false)
    private boolean accepted = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public UUID getActorUserId() { return actorUserId; }
    public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
    public RevisionSource getSource() { return source; }
    public void setSource(RevisionSource source) { this.source = source; }
    public UUID getParentRevisionId() { return parentRevisionId; }
    public void setParentRevisionId(UUID parentRevisionId) { this.parentRevisionId = parentRevisionId; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
