package com.qsource.api.service;

import com.qsource.api.domain.AccessPermission;
import com.qsource.api.domain.OrgRole;
import com.qsource.api.domain.OrganizationMember;
import com.qsource.api.domain.PrincipalType;
import com.qsource.api.domain.Project;
import com.qsource.api.domain.ProjectAccess;
import com.qsource.api.domain.ProjectVisibility;
import com.qsource.api.repo.GroupMemberRepository;
import com.qsource.api.repo.OrganizationMemberRepository;
import com.qsource.api.repo.ProjectAccessRepository;
import com.qsource.api.repo.ProjectRepository;
import com.qsource.api.security.AuthPrincipal;
import com.qsource.api.security.SecurityUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessService {
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessRepository projectAccessRepository;
    private final GroupMemberRepository groupMemberRepository;

    public AccessService(
            OrganizationMemberRepository organizationMemberRepository,
            ProjectRepository projectRepository,
            ProjectAccessRepository projectAccessRepository,
            GroupMemberRepository groupMemberRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
        this.projectRepository = projectRepository;
        this.projectAccessRepository = projectAccessRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public AuthPrincipal principal() {
        return SecurityUtils.requirePrincipal();
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireOrgMember(UUID orgId) {
        AuthPrincipal principal = principal();
        if (principal.apiKey()) {
            if (!orgId.equals(principal.organizationId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key not for this organization");
            }
            OrganizationMember synthetic = new OrganizationMember();
            synthetic.setOrganizationId(orgId);
            synthetic.setRole(OrgRole.MEMBER);
            return synthetic;
        }
        return organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this organization"));
    }

    @Transactional(readOnly = true)
    public OrganizationMember requireOrgAdmin(UUID orgId) {
        OrganizationMember member = requireOrgMember(orgId);
        AuthPrincipal principal = principal();
        if (principal.apiKey()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API keys cannot manage organization");
        }
        if (member.getRole() != OrgRole.OWNER && member.getRole() != OrgRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public Project requireProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    @Transactional(readOnly = true)
    public Project requireProjectAccess(UUID projectId, boolean write) {
        Project project = requireProject(projectId);
        AuthPrincipal principal = principal();

        if (principal.apiKey()) {
            if (!project.getOrganizationId().equals(principal.organizationId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key not for this organization");
            }
            if (principal.projectId() != null && !principal.projectId().equals(projectId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API key not for this project");
            }
            return project;
        }

        OrganizationMember membership = organizationMemberRepository
                .findByOrganizationIdAndUserId(project.getOrganizationId(), principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this organization"));

        boolean admin = membership.getRole() == OrgRole.OWNER || membership.getRole() == OrgRole.ADMIN;
        if (admin) {
            return project;
        }

        if (project.getVisibility() == ProjectVisibility.ORG) {
            return project;
        }

        if (!hasRestrictedAccess(projectId, principal.userId(), write)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this project");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public boolean canSeeProject(Project project, AuthPrincipal principal) {
        if (principal.apiKey()) {
            if (!project.getOrganizationId().equals(principal.organizationId())) {
                return false;
            }
            return principal.projectId() == null || principal.projectId().equals(project.getId());
        }
        var membership = organizationMemberRepository.findByOrganizationIdAndUserId(
                project.getOrganizationId(), principal.userId());
        if (membership.isEmpty()) {
            return false;
        }
        OrgRole role = membership.get().getRole();
        if (role == OrgRole.OWNER || role == OrgRole.ADMIN) {
            return true;
        }
        if (project.getVisibility() == ProjectVisibility.ORG) {
            return true;
        }
        return hasRestrictedAccess(project.getId(), principal.userId(), false);
    }

    private boolean hasRestrictedAccess(UUID projectId, UUID userId, boolean write) {
        List<ProjectAccess> entries = projectAccessRepository.findByProjectId(projectId);
        Set<UUID> groupIds = new HashSet<>();
        for (var gm : groupMemberRepository.findByUserId(userId)) {
            groupIds.add(gm.getGroupId());
        }
        for (ProjectAccess access : entries) {
            if (write && access.getPermission() == AccessPermission.READ) {
                continue;
            }
            if (access.getPrincipalType() == PrincipalType.USER && access.getPrincipalId().equals(userId)) {
                return true;
            }
            if (access.getPrincipalType() == PrincipalType.GROUP && groupIds.contains(access.getPrincipalId())) {
                return true;
            }
        }
        return false;
    }
}
