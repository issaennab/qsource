package com.qsource.api.dto;

import com.qsource.api.domain.RevisionSource;
import com.qsource.api.domain.TaskStatus;
import com.qsource.api.domain.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record OrgResponse(UUID id, String name, Instant createdAt) {}
    public record ProjectResponse(UUID id, UUID organizationId, String name, String description, Instant createdAt) {}
    public record CreateProjectRequest(@NotBlank String name, String description) {}

    public record RoadmapItemResponse(
            UUID id, UUID projectId, String title, String summary, int sequenceIndex, UUID linkedFeatureId) {}
    public record CreateRoadmapItemRequest(@NotBlank String title, String summary, Integer sequenceIndex, UUID linkedFeatureId) {}

    public record FeatureSummary(UUID id, UUID projectId, String title, UUID currentRevisionId, Instant updatedAt) {}
    public record FeatureDetail(
            UUID id,
            UUID projectId,
            String title,
            UUID currentRevisionId,
            Map<String, Object> currentContent,
            Instant updatedAt) {}

    public record CreateFeatureRequest(
            @NotBlank String title,
            String outcome,
            String requirements,
            String successCriteria,
            String changeMessage) {}

    public record PublishFeatureRevisionRequest(
            @NotBlank String title,
            String outcome,
            String requirements,
            String successCriteria,
            @NotBlank String changeMessage) {}

    public record TaskSummary(
            UUID id, UUID projectId, UUID featureId, String title, TaskStatus status, UUID currentRevisionId, Instant updatedAt) {}

    public record TaskDetail(
            UUID id,
            UUID projectId,
            UUID featureId,
            String title,
            TaskStatus status,
            UUID currentRevisionId,
            Map<String, Object> currentContent,
            Instant updatedAt) {}

    public record CreateTaskRequest(
            UUID featureId,
            @NotBlank String title,
            String description,
            TaskStatus status,
            String changeMessage) {}

    public record PublishTaskRevisionRequest(
            @NotBlank String title,
            String description,
            @NotNull TaskStatus status,
            @NotBlank String changeMessage) {}

    public record RevisionResponse(
            UUID id,
            TargetType targetType,
            UUID targetId,
            int version,
            Map<String, Object> content,
            String message,
            String actorName,
            RevisionSource source,
            UUID parentRevisionId,
            boolean accepted,
            Instant createdAt) {}

    public record CommentResponse(
            UUID id, TargetType targetType, UUID targetId, String body, String actorName, RevisionSource source, Instant createdAt) {}

    public record CreateCommentRequest(@NotBlank String body) {}

    public record ActivityResponse(
            UUID id, UUID projectId, String kind, String summary, String actorName, RevisionSource source, UUID relatedId, Instant createdAt) {}

    public record ProjectBoardResponse(
            ProjectResponse project,
            List<RoadmapItemResponse> roadmap,
            List<FeatureSummary> features,
            List<TaskSummary> tasks,
            List<ActivityResponse> activity) {}
}
