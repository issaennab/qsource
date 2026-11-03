package com.qsource.api.dto;

import com.qsource.api.domain.AccessPermission;
import com.qsource.api.domain.OrgRole;
import com.qsource.api.domain.PrincipalType;
import com.qsource.api.domain.ProjectVisibility;
import com.qsource.api.domain.RevisionSource;
import com.qsource.api.domain.TaskStatus;
import com.qsource.api.domain.TargetType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String displayName,
            @NotBlank String organizationName) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthTokens(String accessToken, String refreshToken, String tokenType, UserResponse user) {}

    public record UserResponse(UUID id, String email, String displayName, Instant createdAt) {}

    public record MeResponse(UserResponse user, List<OrgMembershipResponse> organizations) {}

    public record OrgMembershipResponse(UUID organizationId, String name, String slug, OrgRole role) {}

    public record OrgResponse(UUID id, String name, String slug, Instant createdAt) {}

    public record CreateOrgRequest(@NotBlank String name) {}

    public record OrgMemberResponse(UUID userId, String email, String displayName, OrgRole role) {}

    public record AddOrgMemberRequest(@NotBlank @Email String email, @NotNull OrgRole role) {}

    public record UpdateOrgMemberRequest(@NotNull OrgRole role) {}

    public record GroupResponse(UUID id, UUID organizationId, String name, Instant createdAt, List<UUID> memberIds) {}

    public record CreateGroupRequest(@NotBlank String name) {}

    public record AddGroupMemberRequest(@NotNull UUID userId) {}

    public record ProjectResponse(
            UUID id,
            UUID organizationId,
            String name,
            String description,
            ProjectVisibility visibility,
            Instant createdAt) {}

    public record CreateProjectRequest(
            @NotBlank String name, String description, ProjectVisibility visibility) {}

    public record UpdateProjectRequest(String name, String description, ProjectVisibility visibility) {}

    public record AccessPrincipalRequest(
            @NotNull PrincipalType type, @NotNull UUID id, @NotNull AccessPermission permission) {}

    public record ReplaceProjectAccessRequest(@NotNull List<AccessPrincipalRequest> principals) {}

    public record ProjectAccessResponse(
            UUID id, PrincipalType principalType, UUID principalId, AccessPermission permission) {}

    public record TagResponse(UUID id, UUID organizationId, String name, String color, Instant createdAt) {}

    public record CreateTagRequest(@NotBlank String name, String color) {}

    public record UpdateTagRequest(String name, String color) {}

    public record SetTagsRequest(@NotNull List<UUID> tagIds) {}

    public record CreateApiKeyRequest(@NotBlank String name, UUID projectId) {}

    public record ApiKeyCreatedResponse(
            UUID id, UUID organizationId, UUID projectId, String name, String keyPrefix, String apiKey, Instant createdAt) {}

    public record ApiKeyResponse(
            UUID id, UUID organizationId, UUID projectId, String name, String keyPrefix, Instant createdAt) {}

    public record RoadmapItemResponse(
            UUID id, UUID projectId, String title, String summary, int sequenceIndex, UUID linkedFeatureId) {}

    public record CreateRoadmapItemRequest(
            @NotBlank String title, String summary, Integer sequenceIndex, UUID linkedFeatureId) {}

    public record FeatureSummary(
            UUID id,
            UUID projectId,
            String title,
            UUID currentRevisionId,
            Instant updatedAt,
            List<TagResponse> tags) {}

    public record FeatureDetail(
            UUID id,
            UUID projectId,
            String title,
            UUID currentRevisionId,
            Map<String, Object> currentContent,
            Instant updatedAt,
            List<TagResponse> tags) {}

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
            UUID id,
            UUID projectId,
            UUID featureId,
            String title,
            TaskStatus status,
            UUID currentRevisionId,
            Instant updatedAt,
            List<TagResponse> tags) {}

    public record TaskDetail(
            UUID id,
            UUID projectId,
            UUID featureId,
            String title,
            TaskStatus status,
            UUID currentRevisionId,
            Map<String, Object> currentContent,
            Instant updatedAt,
            List<TagResponse> tags) {}

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
            UUID actorUserId,
            RevisionSource source,
            UUID parentRevisionId,
            boolean accepted,
            Instant createdAt) {}

    public record CommentResponse(
            UUID id,
            TargetType targetType,
            UUID targetId,
            String body,
            String actorName,
            UUID actorUserId,
            RevisionSource source,
            Instant createdAt) {}

    public record CreateCommentRequest(@NotBlank String body) {}

    public record ActivityResponse(
            UUID id,
            UUID projectId,
            String kind,
            String summary,
            String actorName,
            UUID actorUserId,
            RevisionSource source,
            UUID relatedId,
            Instant createdAt) {}

    public record ProjectBoardResponse(
            ProjectResponse project,
            List<RoadmapItemResponse> roadmap,
            List<FeatureSummary> features,
            List<TaskSummary> tasks,
            List<ActivityResponse> activity,
            List<TagResponse> orgTags) {}
}
