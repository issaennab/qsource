package com.qsource.api.web;

import com.qsource.api.domain.TargetType;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final ProjectService projectService;

    public TaskController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/{taskId}")
    public TaskDetail get(@PathVariable UUID taskId) {
        return projectService.getTask(taskId);
    }

    @PostMapping("/{taskId}/revisions")
    public TaskDetail publish(@PathVariable UUID taskId, @Valid @RequestBody PublishTaskRevisionRequest request) {
        return projectService.publishTask(taskId, request);
    }

    @GetMapping("/{taskId}/revisions")
    public List<RevisionResponse> history(@PathVariable UUID taskId) {
        return projectService.taskHistory(taskId);
    }

    @GetMapping("/{taskId}/comments")
    public List<CommentResponse> comments(@PathVariable UUID taskId) {
        return projectService.listComments(TargetType.TASK, taskId);
    }

    @PostMapping("/{taskId}/comments")
    public CommentResponse addComment(@PathVariable UUID taskId, @Valid @RequestBody CreateCommentRequest request) {
        return projectService.addComment(TargetType.TASK, taskId, request);
    }

    @PutMapping("/{taskId}/placement")
    public TaskDetail place(@PathVariable UUID taskId, @Valid @RequestBody PlaceTaskRequest request) {
        return projectService.placeTask(taskId, request);
    }
}
