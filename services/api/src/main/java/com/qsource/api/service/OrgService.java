package com.qsource.api.service;

import com.qsource.api.domain.Organization;
import com.qsource.api.domain.OrganizationMember;
import com.qsource.api.domain.OrgRole;
import com.qsource.api.domain.UserAccount;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.OrganizationMemberRepository;
import com.qsource.api.repo.OrganizationRepository;
import com.qsource.api.repo.UserAccountRepository;
import com.qsource.api.security.SecurityUtils;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrgService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserAccountRepository userAccountRepository;
    private final AccessService accessService;
    private final AuthService authService;

    public OrgService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            UserAccountRepository userAccountRepository,
            AccessService accessService,
            AuthService authService) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.userAccountRepository = userAccountRepository;
        this.accessService = accessService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<OrgResponse> listMine() {
        var principal = SecurityUtils.requirePrincipal();
        if (principal.apiKey()) {
            Organization org = organizationRepository
                    .findById(principal.organizationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
            return List.of(toOrg(org));
        }
        return organizationMemberRepository.findByUserIdOrderByCreatedAtAsc(principal.userId()).stream()
                .map(m -> organizationRepository.findById(m.getOrganizationId()).orElse(null))
                .filter(o -> o != null)
                .map(this::toOrg)
                .toList();
    }

    @Transactional
    public OrgResponse create(CreateOrgRequest request) {
        var principal = SecurityUtils.requirePrincipal();
        if (principal.apiKey()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "API keys cannot create organizations");
        }
        Organization org = authService.createOrganization(request.name().trim());
        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(org.getId());
        member.setUserId(principal.userId());
        member.setRole(OrgRole.OWNER);
        organizationMemberRepository.save(member);
        return toOrg(org);
    }

    @Transactional(readOnly = true)
    public OrgResponse get(UUID orgId) {
        accessService.requireOrgMember(orgId);
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return toOrg(org);
    }

    @Transactional(readOnly = true)
    public List<OrgMemberResponse> listMembers(UUID orgId) {
        accessService.requireOrgMember(orgId);
        return organizationMemberRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId).stream()
                .map(m -> {
                    UserAccount user = userAccountRepository.findById(m.getUserId()).orElse(null);
                    if (user == null) {
                        return null;
                    }
                    return new OrgMemberResponse(user.getId(), user.getEmail(), user.getDisplayName(), m.getRole());
                })
                .filter(m -> m != null)
                .toList();
    }

    @Transactional
    public OrgMemberResponse addMember(UUID orgId, AddOrgMemberRequest request) {
        accessService.requireOrgAdmin(orgId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = userAccountRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User must sign up before being added to an organization"));
        if (organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already a member");
        }
        if (request.role() == OrgRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign OWNER via invite");
        }
        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(orgId);
        member.setUserId(user.getId());
        member.setRole(request.role());
        organizationMemberRepository.save(member);
        return new OrgMemberResponse(user.getId(), user.getEmail(), user.getDisplayName(), member.getRole());
    }

    @Transactional
    public OrgMemberResponse updateMember(UUID orgId, UUID userId, UpdateOrgMemberRequest request) {
        accessService.requireOrgAdmin(orgId);
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (member.getRole() == OrgRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change OWNER role");
        }
        if (request.role() == OrgRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot promote to OWNER this way");
        }
        member.setRole(request.role());
        organizationMemberRepository.save(member);
        UserAccount user = userAccountRepository.findById(userId).orElseThrow();
        return new OrgMemberResponse(user.getId(), user.getEmail(), user.getDisplayName(), member.getRole());
    }

    @Transactional
    public void removeMember(UUID orgId, UUID userId) {
        accessService.requireOrgAdmin(orgId);
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (member.getRole() == OrgRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove OWNER");
        }
        organizationMemberRepository.delete(member);
    }

    private OrgResponse toOrg(Organization org) {
        return new OrgResponse(org.getId(), org.getName(), org.getSlug(), org.getCreatedAt());
    }
}
