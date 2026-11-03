package com.qsource.api.web;

import com.qsource.api.domain.TargetType;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/features")
public class FeatureController {
    private final ProjectService projectService;

    public FeatureController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/{featureId}")
    public FeatureDetail get(@PathVariable UUID featureId) {
        return projectService.getFeature(featureId);
    }

    @PostMapping("/{featureId}/revisions")
    public FeatureDetail publish(
            @PathVariable UUID featureId, @Valid @RequestBody PublishFeatureRevisionRequest request) {
        return projectService.publishFeature(featureId, request);
    }

    @GetMapping("/{featureId}/revisions")
    public List<RevisionResponse> history(@PathVariable UUID featureId) {
        return projectService.featureHistory(featureId);
    }

    @GetMapping("/{featureId}/comments")
    public List<CommentResponse> comments(@PathVariable UUID featureId) {
        return projectService.listComments(TargetType.FEATURE, featureId);
    }

    @PostMapping("/{featureId}/comments")
    public CommentResponse addComment(
            @PathVariable UUID featureId, @Valid @RequestBody CreateCommentRequest request) {
        return projectService.addComment(TargetType.FEATURE, featureId, request);
    }
}
