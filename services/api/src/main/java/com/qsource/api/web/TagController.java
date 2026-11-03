package com.qsource.api.web;

import com.qsource.api.dto.Dtos.*;
import com.qsource.api.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TagController {
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PatchMapping("/tags/{tagId}")
    public TagResponse update(@PathVariable UUID tagId, @Valid @RequestBody UpdateTagRequest request) {
        return tagService.update(tagId, request);
    }

    @DeleteMapping("/tags/{tagId}")
    public void delete(@PathVariable UUID tagId) {
        tagService.delete(tagId);
    }

    @PutMapping("/features/{featureId}/tags")
    public List<TagResponse> setFeatureTags(
            @PathVariable UUID featureId, @Valid @RequestBody SetTagsRequest request) {
        return tagService.setFeatureTags(featureId, request);
    }

    @PutMapping("/tasks/{taskId}/tags")
    public List<TagResponse> setTaskTags(@PathVariable UUID taskId, @Valid @RequestBody SetTagsRequest request) {
        return tagService.setTaskTags(taskId, request);
    }
}
