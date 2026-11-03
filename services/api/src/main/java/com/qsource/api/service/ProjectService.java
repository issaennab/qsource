package com.qsource.api.service;

import com.qsource.api.domain.*;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.*;
import com.qsource.api.security.AuthPrincipal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final FeatureRepository featureRepository;
    private final TaskItemRepository taskItemRepository;
    private final ActivityEventRepository activityEventRepository;
    private final CommentRepository commentRepository;
    private final ProjectAccessRepository projectAccessRepository;
    private final RevisionService revisionService;
    private final ActivityService activityService;
    private final AccessService accessService;
    private final TagService tagService;

    public ProjectService(
            ProjectRepository projectRepository,
            RoadmapItemRepository roadmapItemRepository,
            FeatureRepository featureRepository,
            TaskItemRepository taskItemRepository,
            ActivityEventRepository activityEventRepository,
            CommentRepository commentRepository,
            ProjectAccessRepository projectAccessRepository,
            RevisionService revisionService,
            ActivityService activityService,
            AccessService accessService,
            TagService tagService) {
        this.projectRepository = projectRepository;
        this.roadmapItemRepository = roadmapItemRepository;
        this.featureRepository = featureRepository;
        this.taskItemRepository = taskItemRepository;
        this.activityEventRepository = activityEventRepository;
        this.commentRepository = commentRepository;
        this.projectAccessRepository = projectAccessRepository;
        this.revisionService = revisionService;
        this.activityService = activityService;
        this.accessService = accessService;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listVisibleProjects() {
        AuthPrincipal principal = accessService.principal();
        List<Project> candidates;
        if (principal.apiKey()) {
            candidates = projectRepository.findByOrganizationIdOrderByCreatedAtAsc(principal.organizationId());
        } else {
            candidates = projectRepository.findAll();
        }
        List<ProjectResponse> result = new ArrayList<>();
        for (Project project : candidates) {
            if (accessService.canSeeProject(project, principal)) {
                result.add(toProject(project));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listOrgProjects(UUID orgId) {
        accessService.requireOrgMember(orgId);
        AuthPrincipal principal = accessService.principal();
        return projectRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId).stream()
                .filter(p -> accessService.canSeeProject(p, principal))
                .map(this::toProject)
                .toList();
    }

    @Transactional
    public ProjectResponse createOrgProject(UUID orgId, CreateProjectRequest request) {
        accessService.requireOrgMember(orgId);
        Project project = new Project();
        project.setOrganizationId(orgId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setVisibility(request.visibility() == null ? ProjectVisibility.ORG : request.visibility());
        project = projectRepository.save(project);
        activityService.record(project.getId(), "PROJECT_CREATED", "Created project " + project.getName(), project.getId());
        return toProject(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request) {
        Project project = accessService.requireProjectAccess(projectId, true);
        accessService.requireOrgAdmin(project.getOrganizationId());
        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.visibility() != null) {
            project.setVisibility(request.visibility());
        }
        return toProject(projectRepository.save(project));
    }

    @Transactional
    public List<ProjectAccessResponse> replaceAccess(UUID projectId, ReplaceProjectAccessRequest request) {
        Project project = accessService.requireProject(projectId);
        accessService.requireOrgAdmin(project.getOrganizationId());
        projectAccessRepository.deleteByProjectId(projectId);
        List<ProjectAccessResponse> saved = new ArrayList<>();
        for (AccessPrincipalRequest principal : request.principals()) {
            ProjectAccess access = new ProjectAccess();
            access.setProjectId(projectId);
            access.setPrincipalType(principal.type());
            access.setPrincipalId(principal.id());
            access.setPermission(principal.permission());
            access = projectAccessRepository.save(access);
            saved.add(new ProjectAccessResponse(
                    access.getId(), access.getPrincipalType(), access.getPrincipalId(), access.getPermission()));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProjectAccessResponse> listAccess(UUID projectId) {
        Project project = accessService.requireProject(projectId);
        accessService.requireOrgAdmin(project.getOrganizationId());
        return projectAccessRepository.findByProjectId(projectId).stream()
                .map(a -> new ProjectAccessResponse(
                        a.getId(), a.getPrincipalType(), a.getPrincipalId(), a.getPermission()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectBoardResponse board(UUID projectId, UUID tagId, String tagName) {
        Project project = accessService.requireProjectAccess(projectId, false);
        Set<UUID> filterFeatureIds = null;
        Set<UUID> filterTaskIds = null;
        if (tagId != null || (tagName != null && !tagName.isBlank())) {
            UUID resolvedTagId = tagId;
            if (resolvedTagId == null) {
                resolvedTagId = tagService.requireOrgTagByName(project.getOrganizationId(), tagName).getId();
            } else {
                UUID finalTagId = resolvedTagId;
                boolean ok = tagService.list(project.getOrganizationId()).stream()
                        .anyMatch(t -> t.id().equals(finalTagId));
                if (!ok) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
                }
            }
            Set<UUID> tagged = tagService.targetIdsWithTag(resolvedTagId);
            filterFeatureIds = tagged;
            filterTaskIds = tagged;
        }

        List<FeatureSummary> features = new ArrayList<>();
        for (Feature f : featureRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)) {
            if (filterFeatureIds != null && !filterFeatureIds.contains(f.getId())) {
                continue;
            }
            features.add(toFeatureSummary(f));
        }
        List<TaskSummary> tasks = new ArrayList<>();
        for (TaskItem t : taskItemRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)) {
            if (filterTaskIds != null && !filterTaskIds.contains(t.getId())) {
                continue;
            }
            tasks.add(toTaskSummary(t));
        }

        return new ProjectBoardResponse(
                toProject(project),
                roadmapItemRepository.findByProjectIdOrderBySequenceIndexAsc(projectId).stream()
                        .map(this::toRoadmap)
                        .toList(),
                features,
                tasks,
                activityEventRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                        .limit(50)
                        .map(this::toActivity)
                        .toList(),
                tagService.list(project.getOrganizationId()));
    }

    @Transactional
    public RoadmapItemResponse addRoadmapItem(UUID projectId, CreateRoadmapItemRequest request) {
        accessService.requireProjectAccess(projectId, true);
        int index = request.sequenceIndex() != null
                ? request.sequenceIndex()
                : roadmapItemRepository.findByProjectIdOrderBySequenceIndexAsc(projectId).size();
        RoadmapItem item = new RoadmapItem();
        item.setProjectId(projectId);
        item.setTitle(request.title());
        item.setSummary(request.summary());
        item.setSequenceIndex(index);
        item.setLinkedFeatureId(request.linkedFeatureId());
        item = roadmapItemRepository.save(item);
        activityService.record(projectId, "ROADMAP_ADDED", "Added roadmap item: " + item.getTitle(), item.getId());
        return toRoadmap(item);
    }

    @Transactional
    public FeatureDetail createFeature(UUID projectId, CreateFeatureRequest request) {
        accessService.requireProjectAccess(projectId, true);
        Feature feature = new Feature();
        feature.setProjectId(projectId);
        feature.setTitle(request.title());
        feature = featureRepository.save(feature);

        Map<String, Object> content = featureContent(
                request.title(), request.outcome(), request.requirements(), request.successCriteria());
        String message = request.changeMessage() == null || request.changeMessage().isBlank()
                ? "Initial revision"
                : request.changeMessage();
        Revision revision = revisionService.publish(TargetType.FEATURE, feature.getId(), null, content, message);
        feature.setCurrentRevisionId(revision.getId());
        feature.setUpdatedAt(revision.getCreatedAt());
        featureRepository.save(feature);
        activityService.record(projectId, "FEATURE_CREATED", "Created feature: " + feature.getTitle(), feature.getId());
        return toFeatureDetail(feature, revision);
    }

    @Transactional(readOnly = true)
    public FeatureDetail getFeature(UUID featureId) {
        Feature feature = requireFeature(featureId);
        accessService.requireProjectAccess(feature.getProjectId(), false);
        Revision current = feature.getCurrentRevisionId() == null
                ? null
                : revisionService.require(feature.getCurrentRevisionId());
        return toFeatureDetail(feature, current);
    }

    @Transactional
    public FeatureDetail publishFeature(UUID featureId, PublishFeatureRevisionRequest request) {
        Feature feature = requireFeature(featureId);
        accessService.requireProjectAccess(feature.getProjectId(), true);
        Map<String, Object> content = featureContent(
                request.title(), request.outcome(), request.requirements(), request.successCriteria());
        Revision revision = revisionService.publish(
                TargetType.FEATURE, feature.getId(), feature.getCurrentRevisionId(), content, request.changeMessage());
        feature.setTitle(request.title());
        feature.setCurrentRevisionId(revision.getId());
        feature.setUpdatedAt(revision.getCreatedAt());
        featureRepository.save(feature);
        activityService.record(
                feature.getProjectId(),
                "FEATURE_REVISION",
                "Published v" + revision.getVersion() + " of " + feature.getTitle(),
                revision.getId());
        return toFeatureDetail(feature, revision);
    }

    @Transactional(readOnly = true)
    public List<RevisionResponse> featureHistory(UUID featureId) {
        Feature feature = requireFeature(featureId);
        accessService.requireProjectAccess(feature.getProjectId(), false);
        return revisionService.history(TargetType.FEATURE, featureId).stream().map(this::toRevision).toList();
    }

    @Transactional
    public TaskDetail createTask(UUID projectId, CreateTaskRequest request) {
        accessService.requireProjectAccess(projectId, true);
        if (request.featureId() != null) {
            Feature feature = requireFeature(request.featureId());
            if (!feature.getProjectId().equals(projectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feature not in project");
            }
        }
        TaskItem task = new TaskItem();
        task.setProjectId(projectId);
        task.setFeatureId(request.featureId());
        task.setTitle(request.title());
        task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
        task = taskItemRepository.save(task);

        Map<String, Object> content = taskContent(request.title(), request.description(), task.getStatus());
        String message = request.changeMessage() == null || request.changeMessage().isBlank()
                ? "Initial revision"
                : request.changeMessage();
        Revision revision = revisionService.publish(TargetType.TASK, task.getId(), null, content, message);
        task.setCurrentRevisionId(revision.getId());
        task.setUpdatedAt(revision.getCreatedAt());
        taskItemRepository.save(task);
        activityService.record(projectId, "TASK_CREATED", "Created task: " + task.getTitle(), task.getId());
        return toTaskDetail(task, revision);
    }

    @Transactional(readOnly = true)
    public TaskDetail getTask(UUID taskId) {
        TaskItem task = requireTask(taskId);
        accessService.requireProjectAccess(task.getProjectId(), false);
        Revision current = task.getCurrentRevisionId() == null ? null : revisionService.require(task.getCurrentRevisionId());
        return toTaskDetail(task, current);
    }

    @Transactional
    public TaskDetail publishTask(UUID taskId, PublishTaskRevisionRequest request) {
        TaskItem task = requireTask(taskId);
        accessService.requireProjectAccess(task.getProjectId(), true);
        Map<String, Object> content = taskContent(request.title(), request.description(), request.status());
        Revision revision = revisionService.publish(
                TargetType.TASK, task.getId(), task.getCurrentRevisionId(), content, request.changeMessage());
        task.setTitle(request.title());
        task.setStatus(request.status());
        task.setCurrentRevisionId(revision.getId());
        task.setUpdatedAt(revision.getCreatedAt());
        taskItemRepository.save(task);
        activityService.record(
                task.getProjectId(),
                "TASK_REVISION",
                "Published v" + revision.getVersion() + " of " + task.getTitle(),
                revision.getId());
        return toTaskDetail(task, revision);
    }

    @Transactional(readOnly = true)
    public List<RevisionResponse> taskHistory(UUID taskId) {
        TaskItem task = requireTask(taskId);
        accessService.requireProjectAccess(task.getProjectId(), false);
        return revisionService.history(TargetType.TASK, taskId).stream().map(this::toRevision).toList();
    }

    @Transactional
    public CommentResponse addComment(TargetType type, UUID targetId, CreateCommentRequest request) {
        UUID projectId;
        if (type == TargetType.FEATURE) {
            projectId = requireFeature(targetId).getProjectId();
        } else {
            projectId = requireTask(targetId).getProjectId();
        }
        accessService.requireProjectAccess(projectId, true);
        var actor = com.qsource.api.config.ActorContext.get();
        Comment comment = new Comment();
        comment.setTargetType(type);
        comment.setTargetId(targetId);
        comment.setBody(request.body());
        comment.setActorName(actor.name());
        comment.setActorUserId(actor.userId());
        comment.setSource(actor.source());
        comment = commentRepository.save(comment);
        activityService.record(projectId, "COMMENT_ADDED", "Comment on " + type.name().toLowerCase(), comment.getId());
        return new CommentResponse(
                comment.getId(),
                comment.getTargetType(),
                comment.getTargetId(),
                comment.getBody(),
                comment.getActorName(),
                comment.getActorUserId(),
                comment.getSource(),
                comment.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(TargetType type, UUID targetId) {
        UUID projectId;
        if (type == TargetType.FEATURE) {
            projectId = requireFeature(targetId).getProjectId();
        } else {
            projectId = requireTask(targetId).getProjectId();
        }
        accessService.requireProjectAccess(projectId, false);
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(type, targetId).stream()
                .map(c -> new CommentResponse(
                        c.getId(),
                        c.getTargetType(),
                        c.getTargetId(),
                        c.getBody(),
                        c.getActorName(),
                        c.getActorUserId(),
                        c.getSource(),
                        c.getCreatedAt()))
                .toList();
    }

    Feature requireFeature(UUID id) {
        return featureRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
    }

    TaskItem requireTask(UUID id) {
        return taskItemRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private Map<String, Object> featureContent(String title, String outcome, String requirements, String successCriteria) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("outcome", outcome == null ? "" : outcome);
        map.put("requirements", requirements == null ? "" : requirements);
        map.put("successCriteria", successCriteria == null ? "" : successCriteria);
        return map;
    }

    private Map<String, Object> taskContent(String title, String description, TaskStatus status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("description", description == null ? "" : description);
        map.put("status", status.name());
        return map;
    }

    ProjectResponse toProject(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getOrganizationId(),
                p.getName(),
                p.getDescription(),
                p.getVisibility() == null ? ProjectVisibility.ORG : p.getVisibility(),
                p.getCreatedAt());
    }

    RoadmapItemResponse toRoadmap(RoadmapItem i) {
        return new RoadmapItemResponse(
                i.getId(), i.getProjectId(), i.getTitle(), i.getSummary(), i.getSequenceIndex(), i.getLinkedFeatureId());
    }

    FeatureSummary toFeatureSummary(Feature f) {
        return new FeatureSummary(
                f.getId(),
                f.getProjectId(),
                f.getTitle(),
                f.getCurrentRevisionId(),
                f.getUpdatedAt(),
                tagService.tagsFor(TargetType.FEATURE, f.getId()));
    }

    FeatureDetail toFeatureDetail(Feature f, Revision revision) {
        Map<String, Object> content = revision == null ? Map.of() : revisionService.contentOf(revision);
        return new FeatureDetail(
                f.getId(),
                f.getProjectId(),
                f.getTitle(),
                f.getCurrentRevisionId(),
                content,
                f.getUpdatedAt(),
                tagService.tagsFor(TargetType.FEATURE, f.getId()));
    }

    TaskSummary toTaskSummary(TaskItem t) {
        return new TaskSummary(
                t.getId(),
                t.getProjectId(),
                t.getFeatureId(),
                t.getTitle(),
                t.getStatus(),
                t.getCurrentRevisionId(),
                t.getUpdatedAt(),
                tagService.tagsFor(TargetType.TASK, t.getId()));
    }

    TaskDetail toTaskDetail(TaskItem t, Revision revision) {
        Map<String, Object> content = revision == null ? Map.of() : revisionService.contentOf(revision);
        return new TaskDetail(
                t.getId(),
                t.getProjectId(),
                t.getFeatureId(),
                t.getTitle(),
                t.getStatus(),
                t.getCurrentRevisionId(),
                content,
                t.getUpdatedAt(),
                tagService.tagsFor(TargetType.TASK, t.getId()));
    }

    RevisionResponse toRevision(Revision r) {
        return new RevisionResponse(
                r.getId(),
                r.getTargetType(),
                r.getTargetId(),
                r.getVersion(),
                revisionService.contentOf(r),
                r.getMessage(),
                r.getActorName(),
                r.getActorUserId(),
                r.getSource(),
                r.getParentRevisionId(),
                r.isAccepted(),
                r.getCreatedAt());
    }

    ActivityResponse toActivity(ActivityEvent e) {
        return new ActivityResponse(
                e.getId(),
                e.getProjectId(),
                e.getKind(),
                e.getSummary(),
                e.getActorName(),
                e.getActorUserId(),
                e.getSource(),
                e.getRelatedId(),
                e.getCreatedAt());
    }
}
