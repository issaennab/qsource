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
        return projectService.listProjects();
    }

    @PostMapping("/projects")
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping("/projects/{projectId}/board")
    public ProjectBoardResponse board(@PathVariable UUID projectId) {
        return projectService.board(projectId);
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
