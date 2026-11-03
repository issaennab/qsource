package com.qsource.api.service;

import com.qsource.api.domain.Feature;
import com.qsource.api.domain.Tag;
import com.qsource.api.domain.Tagging;
import com.qsource.api.domain.TaskItem;
import com.qsource.api.domain.TargetType;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.FeatureRepository;
import com.qsource.api.repo.ProjectRepository;
import com.qsource.api.repo.TagRepository;
import com.qsource.api.repo.TaggingRepository;
import com.qsource.api.repo.TaskItemRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TagService {
    private final TagRepository tagRepository;
    private final TaggingRepository taggingRepository;
    private final AccessService accessService;
    private final FeatureRepository featureRepository;
    private final TaskItemRepository taskItemRepository;
    private final ProjectRepository projectRepository;

    public TagService(
            TagRepository tagRepository,
            TaggingRepository taggingRepository,
            AccessService accessService,
            FeatureRepository featureRepository,
            TaskItemRepository taskItemRepository,
            ProjectRepository projectRepository) {
        this.tagRepository = tagRepository;
        this.taggingRepository = taggingRepository;
        this.accessService = accessService;
        this.featureRepository = featureRepository;
        this.taskItemRepository = taskItemRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list(UUID orgId) {
        accessService.requireOrgMember(orgId);
        return tagRepository.findByOrganizationIdOrderByNameAsc(orgId).stream().map(this::toTag).toList();
    }

    @Transactional
    public TagResponse create(UUID orgId, CreateTagRequest request) {
        accessService.requireOrgAdmin(orgId);
        String name = request.name().trim();
        if (tagRepository.findByOrganizationIdAndNameIgnoreCase(orgId, name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag already exists");
        }
        Tag tag = new Tag();
        tag.setOrganizationId(orgId);
        tag.setName(name);
        tag.setColor(request.color());
        return toTag(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(UUID tagId, UpdateTagRequest request) {
        Tag tag = requireTag(tagId);
        accessService.requireOrgAdmin(tag.getOrganizationId());
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            tagRepository
                    .findByOrganizationIdAndNameIgnoreCase(tag.getOrganizationId(), name)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(tagId)) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag already exists");
                        }
                    });
            tag.setName(name);
        }
        if (request.color() != null) {
            tag.setColor(request.color());
        }
        return toTag(tagRepository.save(tag));
    }

    @Transactional
    public void delete(UUID tagId) {
        Tag tag = requireTag(tagId);
        accessService.requireOrgAdmin(tag.getOrganizationId());
        taggingRepository.deleteByTagId(tagId);
        tagRepository.delete(tag);
    }

    @Transactional
    public List<TagResponse> setFeatureTags(UUID featureId, SetTagsRequest request) {
        Feature feature = featureRepository
                .findById(featureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        accessService.requireProjectAccess(feature.getProjectId(), true);
        UUID orgId = projectRepository.findById(feature.getProjectId()).orElseThrow().getOrganizationId();
        return replaceTags(TargetType.FEATURE, featureId, orgId, request.tagIds());
    }

    @Transactional
    public List<TagResponse> setTaskTags(UUID taskId, SetTagsRequest request) {
        TaskItem task = taskItemRepository
                .findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        accessService.requireProjectAccess(task.getProjectId(), true);
        UUID orgId = projectRepository.findById(task.getProjectId()).orElseThrow().getOrganizationId();
        return replaceTags(TargetType.TASK, taskId, orgId, request.tagIds());
    }

    @Transactional(readOnly = true)
    public List<TagResponse> tagsFor(TargetType type, UUID targetId) {
        List<Tagging> taggings = taggingRepository.findByTargetTypeAndTargetId(type, targetId);
        List<TagResponse> tags = new ArrayList<>();
        for (Tagging tagging : taggings) {
            tagRepository.findById(tagging.getTagId()).ifPresent(tag -> tags.add(toTag(tag)));
        }
        return tags;
    }

    @Transactional(readOnly = true)
    public Set<UUID> targetIdsWithTag(UUID tagId) {
        Set<UUID> ids = new HashSet<>();
        for (Tagging tagging : taggingRepository.findByTagId(tagId)) {
            ids.add(tagging.getTargetId());
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public Tag requireOrgTagByName(UUID orgId, String name) {
        return tagRepository
                .findByOrganizationIdAndNameIgnoreCase(orgId, name.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    private List<TagResponse> replaceTags(TargetType type, UUID targetId, UUID orgId, List<UUID> tagIds) {
        Set<UUID> unique = new HashSet<>(tagIds == null ? List.of() : tagIds);
        for (UUID tagId : unique) {
            Tag tag = requireTag(tagId);
            if (!tag.getOrganizationId().equals(orgId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag not in organization");
            }
        }
        taggingRepository.deleteByTargetTypeAndTargetId(type, targetId);
        for (UUID tagId : unique) {
            Tagging tagging = new Tagging();
            tagging.setTagId(tagId);
            tagging.setTargetType(type);
            tagging.setTargetId(targetId);
            taggingRepository.save(tagging);
        }
        return tagsFor(type, targetId);
    }

    private Tag requireTag(UUID tagId) {
        return tagRepository
                .findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    public TagResponse toTag(Tag tag) {
        return new TagResponse(tag.getId(), tag.getOrganizationId(), tag.getName(), tag.getColor(), tag.getCreatedAt());
    }
}
