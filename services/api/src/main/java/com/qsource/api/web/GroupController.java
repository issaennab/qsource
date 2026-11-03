package com.qsource.api.web;

import com.qsource.api.dto.Dtos.*;
import com.qsource.api.service.GroupService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable UUID groupId) {
        return groupService.get(groupId);
    }

    @PostMapping("/{groupId}/members")
    public GroupResponse addMember(@PathVariable UUID groupId, @Valid @RequestBody AddGroupMemberRequest request) {
        return groupService.addMember(groupId, request);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public void removeMember(@PathVariable UUID groupId, @PathVariable UUID userId) {
        groupService.removeMember(groupId, userId);
    }

    @DeleteMapping("/{groupId}")
    public void delete(@PathVariable UUID groupId) {
        groupService.delete(groupId);
    }
}
