package com.qsource.api.web;

import com.qsource.api.dto.Dtos.*;
import com.qsource.api.service.ApiKeyService;
import com.qsource.api.service.GroupService;
import com.qsource.api.service.OrgService;
import com.qsource.api.service.ProjectService;
import com.qsource.api.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrgController {
    private final OrgService orgService;
    private final GroupService groupService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final ApiKeyService apiKeyService;

    public OrgController(
            OrgService orgService,
            GroupService groupService,
            ProjectService projectService,
            TagService tagService,
            ApiKeyService apiKeyService) {
        this.orgService = orgService;
        this.groupService = groupService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.apiKeyService = apiKeyService;
    }

    @GetMapping("/orgs")
    public List<OrgResponse> listOrgs() {
        return orgService.listMine();
    }

    @PostMapping("/orgs")
    public OrgResponse createOrg(@Valid @RequestBody CreateOrgRequest request) {
        return orgService.create(request);
    }

    @GetMapping("/orgs/{orgId}")
    public OrgResponse getOrg(@PathVariable UUID orgId) {
        return orgService.get(orgId);
    }

    @GetMapping("/orgs/{orgId}/members")
    public List<OrgMemberResponse> members(@PathVariable UUID orgId) {
        return orgService.listMembers(orgId);
    }

    @PostMapping("/orgs/{orgId}/members")
    public OrgMemberResponse addMember(@PathVariable UUID orgId, @Valid @RequestBody AddOrgMemberRequest request) {
        return orgService.addMember(orgId, request);
    }

    @PatchMapping("/orgs/{orgId}/members/{userId}")
    public OrgMemberResponse updateMember(
            @PathVariable UUID orgId, @PathVariable UUID userId, @Valid @RequestBody UpdateOrgMemberRequest request) {
        return orgService.updateMember(orgId, userId, request);
    }

    @DeleteMapping("/orgs/{orgId}/members/{userId}")
    public void removeMember(@PathVariable UUID orgId, @PathVariable UUID userId) {
        orgService.removeMember(orgId, userId);
    }

    @GetMapping("/orgs/{orgId}/groups")
    public List<GroupResponse> groups(@PathVariable UUID orgId) {
        return groupService.list(orgId);
    }

    @PostMapping("/orgs/{orgId}/groups")
    public GroupResponse createGroup(@PathVariable UUID orgId, @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(orgId, request);
    }

    @GetMapping("/orgs/{orgId}/projects")
    public List<ProjectResponse> orgProjects(@PathVariable UUID orgId) {
        return projectService.listOrgProjects(orgId);
    }

    @PostMapping("/orgs/{orgId}/projects")
    public ProjectResponse createProject(@PathVariable UUID orgId, @Valid @RequestBody CreateProjectRequest request) {
        return projectService.createOrgProject(orgId, request);
    }

    @GetMapping("/orgs/{orgId}/tags")
    public List<TagResponse> tags(@PathVariable UUID orgId) {
        return tagService.list(orgId);
    }

    @PostMapping("/orgs/{orgId}/tags")
    public TagResponse createTag(@PathVariable UUID orgId, @Valid @RequestBody CreateTagRequest request) {
        return tagService.create(orgId, request);
    }

    @GetMapping("/orgs/{orgId}/api-keys")
    public List<ApiKeyResponse> apiKeys(@PathVariable UUID orgId) {
        return apiKeyService.list(orgId);
    }

    @PostMapping("/orgs/{orgId}/api-keys")
    public ApiKeyCreatedResponse createApiKey(
            @PathVariable UUID orgId, @Valid @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.create(orgId, request);
    }

    @DeleteMapping("/orgs/{orgId}/api-keys/{keyId}")
    public void revokeApiKey(@PathVariable UUID orgId, @PathVariable UUID keyId) {
        apiKeyService.revoke(orgId, keyId);
    }
}
