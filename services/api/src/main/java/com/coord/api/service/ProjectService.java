package com.coord.api.service;

import com.coord.api.domain.*;
import com.coord.api.dto.Dtos.*;
import com.coord.api.repo.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final FeatureRepository featureRepository;
    private final TaskItemRepository taskItemRepository;
    private final ActivityEventRepository activityEventRepository;
    private final CommentRepository commentRepository;
    private final RevisionService revisionService;
    private final ActivityService activityService;

    public ProjectService(
            OrganizationRepository organizationRepository,
            ProjectRepository projectRepository,
            RoadmapItemRepository roadmapItemRepository,
            FeatureRepository featureRepository,
            TaskItemRepository taskItemRepository,
            ActivityEventRepository activityEventRepository,
            CommentRepository commentRepository,
            RevisionService revisionService,
            ActivityService activityService) {
        this.organizationRepository = organizationRepository;
        this.projectRepository = projectRepository;
        this.roadmapItemRepository = roadmapItemRepository;
        this.featureRepository = featureRepository;
        this.taskItemRepository = taskItemRepository;
        this.activityEventRepository = activityEventRepository;
        this.commentRepository = commentRepository;
        this.revisionService = revisionService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        return projectRepository.findAll().stream().map(this::toProject).toList();
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        Organization org = organizationRepository.findAll().stream().findFirst().orElseGet(() -> {
            Organization created = new Organization();
            created.setName("Personal");
            return organizationRepository.save(created);
        });
        Project project = new Project();
        project.setOrganizationId(org.getId());
        project.setName(request.name());
        project.setDescription(request.description());
        project = projectRepository.save(project);
        activityService.record(project.getId(), "PROJECT_CREATED", "Created project " + project.getName(), project.getId());
        return toProject(project);
    }

    @Transactional(readOnly = true)
    public ProjectBoardResponse board(UUID projectId) {
        Project project = requireProject(projectId);
        return new ProjectBoardResponse(
                toProject(project),
                roadmapItemRepository.findByProjectIdOrderBySequenceIndexAsc(projectId).stream()
                        .map(this::toRoadmap)
                        .toList(),
                featureRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                        .map(this::toFeatureSummary)
                        .toList(),
                taskItemRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                        .map(this::toTaskSummary)
                        .toList(),
                activityEventRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                        .limit(50)
                        .map(this::toActivity)
                        .toList());
    }

    @Transactional
    public RoadmapItemResponse addRoadmapItem(UUID projectId, CreateRoadmapItemRequest request) {
        requireProject(projectId);
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
        requireProject(projectId);
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
        Revision current = feature.getCurrentRevisionId() == null
                ? null
                : revisionService.require(feature.getCurrentRevisionId());
        return toFeatureDetail(feature, current);
    }

    @Transactional
    public FeatureDetail publishFeature(UUID featureId, PublishFeatureRevisionRequest request) {
        Feature feature = requireFeature(featureId);
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
        requireFeature(featureId);
        return revisionService.history(TargetType.FEATURE, featureId).stream().map(this::toRevision).toList();
    }

    @Transactional
    public TaskDetail createTask(UUID projectId, CreateTaskRequest request) {
        requireProject(projectId);
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
        Revision current = task.getCurrentRevisionId() == null ? null : revisionService.require(task.getCurrentRevisionId());
        return toTaskDetail(task, current);
    }

    @Transactional
    public TaskDetail publishTask(UUID taskId, PublishTaskRevisionRequest request) {
        TaskItem task = requireTask(taskId);
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
        requireTask(taskId);
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
        var actor = com.coord.api.config.ActorContext.get();
        Comment comment = new Comment();
        comment.setTargetType(type);
        comment.setTargetId(targetId);
        comment.setBody(request.body());
        comment.setActorName(actor.name());
        comment.setSource(actor.source());
        comment = commentRepository.save(comment);
        activityService.record(projectId, "COMMENT_ADDED", "Comment on " + type.name().toLowerCase(), comment.getId());
        return new CommentResponse(
                comment.getId(),
                comment.getTargetType(),
                comment.getTargetId(),
                comment.getBody(),
                comment.getActorName(),
                comment.getSource(),
                comment.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(TargetType type, UUID targetId) {
        if (type == TargetType.FEATURE) {
            requireFeature(targetId);
        } else {
            requireTask(targetId);
        }
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(type, targetId).stream()
                .map(c -> new CommentResponse(
                        c.getId(),
                        c.getTargetType(),
                        c.getTargetId(),
                        c.getBody(),
                        c.getActorName(),
                        c.getSource(),
                        c.getCreatedAt()))
                .toList();
    }

    Project requireProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    Feature requireFeature(UUID id) {
        return featureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
    }

    TaskItem requireTask(UUID id) {
        return taskItemRepository.findById(id)
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
        return new ProjectResponse(p.getId(), p.getOrganizationId(), p.getName(), p.getDescription(), p.getCreatedAt());
    }

    RoadmapItemResponse toRoadmap(RoadmapItem i) {
        return new RoadmapItemResponse(
                i.getId(), i.getProjectId(), i.getTitle(), i.getSummary(), i.getSequenceIndex(), i.getLinkedFeatureId());
    }

    FeatureSummary toFeatureSummary(Feature f) {
        return new FeatureSummary(f.getId(), f.getProjectId(), f.getTitle(), f.getCurrentRevisionId(), f.getUpdatedAt());
    }

    FeatureDetail toFeatureDetail(Feature f, Revision revision) {
        Map<String, Object> content = revision == null ? Map.of() : revisionService.contentOf(revision);
        return new FeatureDetail(
                f.getId(), f.getProjectId(), f.getTitle(), f.getCurrentRevisionId(), content, f.getUpdatedAt());
    }

    TaskSummary toTaskSummary(TaskItem t) {
        return new TaskSummary(
                t.getId(), t.getProjectId(), t.getFeatureId(), t.getTitle(), t.getStatus(), t.getCurrentRevisionId(), t.getUpdatedAt());
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
                t.getUpdatedAt());
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
                r.getSource(),
                r.getParentRevisionId(),
                r.isAccepted(),
                r.getCreatedAt());
    }

    ActivityResponse toActivity(ActivityEvent e) {
        return new ActivityResponse(
                e.getId(), e.getProjectId(), e.getKind(), e.getSummary(), e.getActorName(), e.getSource(), e.getRelatedId(), e.getCreatedAt());
    }
}
