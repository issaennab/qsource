package com.qsource.api.service;

import com.qsource.api.domain.GroupMember;
import com.qsource.api.domain.OrgGroup;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.GroupMemberRepository;
import com.qsource.api.repo.OrgGroupRepository;
import com.qsource.api.repo.OrganizationMemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupService {
    private final OrgGroupRepository orgGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AccessService accessService;

    public GroupService(
            OrgGroupRepository orgGroupRepository,
            GroupMemberRepository groupMemberRepository,
            OrganizationMemberRepository organizationMemberRepository,
            AccessService accessService) {
        this.orgGroupRepository = orgGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> list(UUID orgId) {
        accessService.requireOrgMember(orgId);
        return orgGroupRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(this::toGroup)
                .toList();
    }

    @Transactional
    public GroupResponse create(UUID orgId, CreateGroupRequest request) {
        accessService.requireOrgAdmin(orgId);
        OrgGroup group = new OrgGroup();
        group.setOrganizationId(orgId);
        group.setName(request.name().trim());
        group = orgGroupRepository.save(group);
        return toGroup(group);
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID groupId) {
        OrgGroup group = requireGroup(groupId);
        accessService.requireOrgMember(group.getOrganizationId());
        return toGroup(group);
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, AddGroupMemberRequest request) {
        OrgGroup group = requireGroup(groupId);
        accessService.requireOrgAdmin(group.getOrganizationId());
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(
                group.getOrganizationId(), request.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be an org member");
        }
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, request.userId()).isEmpty()) {
            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserId(request.userId());
            groupMemberRepository.save(member);
        }
        return toGroup(group);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        OrgGroup group = requireGroup(groupId);
        accessService.requireOrgAdmin(group.getOrganizationId());
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
    }

    @Transactional
    public void delete(UUID groupId) {
        OrgGroup group = requireGroup(groupId);
        accessService.requireOrgAdmin(group.getOrganizationId());
        groupMemberRepository.deleteByGroupId(groupId);
        orgGroupRepository.delete(group);
    }

    private OrgGroup requireGroup(UUID groupId) {
        return orgGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private GroupResponse toGroup(OrgGroup group) {
        List<UUID> memberIds = groupMemberRepository.findByGroupId(group.getId()).stream()
                .map(GroupMember::getUserId)
                .toList();
        return new GroupResponse(
                group.getId(), group.getOrganizationId(), group.getName(), group.getCreatedAt(), memberIds);
    }
}
