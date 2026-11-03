package com.qsource.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_access", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"projectId", "principalType", "principalId"})
})
public class ProjectAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrincipalType principalType;

    @Column(nullable = false)
    private UUID principalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessPermission permission = AccessPermission.WRITE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public PrincipalType getPrincipalType() { return principalType; }
    public void setPrincipalType(PrincipalType principalType) { this.principalType = principalType; }
    public UUID getPrincipalId() { return principalId; }
    public void setPrincipalId(UUID principalId) { this.principalId = principalId; }
    public AccessPermission getPermission() { return permission; }
    public void setPermission(AccessPermission permission) { this.permission = permission; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
