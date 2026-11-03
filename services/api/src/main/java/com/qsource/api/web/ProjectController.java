package com.qsource.api.web;

import com.qsource.api.dto.Dtos.*;
import com.qsource.api.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public List<ProjectResponse> listProjects() {
        return projectService.listVisibleProjects();
    }

    @GetMapping("/projects/{projectId}/board")
    public ProjectBoardResponse board(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID tagId,
            @RequestParam(required = false) String tag) {
        return projectService.board(projectId, tagId, tag);
    }

    @PatchMapping("/projects/{projectId}")
    public ProjectResponse updateProject(
            @PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.updateProject(projectId, request);
    }

    @GetMapping("/projects/{projectId}/access")
    public List<ProjectAccessResponse> listAccess(@PathVariable UUID projectId) {
        return projectService.listAccess(projectId);
    }

    @PutMapping("/projects/{projectId}/access")
    public List<ProjectAccessResponse> replaceAccess(
            @PathVariable UUID projectId, @Valid @RequestBody ReplaceProjectAccessRequest request) {
        return projectService.replaceAccess(projectId, request);
    }

    @PostMapping("/projects/{projectId}/roadmap")
    public RoadmapItemResponse addRoadmap(
            @PathVariable UUID projectId, @Valid @RequestBody CreateRoadmapItemRequest request) {
        return projectService.addRoadmapItem(projectId, request);
    }

    @PostMapping("/projects/{projectId}/features")
    public FeatureDetail createFeature(
            @PathVariable UUID projectId, @Valid @RequestBody CreateFeatureRequest request) {
        return projectService.createFeature(projectId, request);
    }

    @PostMapping("/projects/{projectId}/tasks")
    public TaskDetail createTask(@PathVariable UUID projectId, @Valid @RequestBody CreateTaskRequest request) {
        return projectService.createTask(projectId, request);
    }
}
