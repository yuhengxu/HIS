package com.health.platform.iam;

import java.util.List;
import java.util.Set;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam")
public class PermissionController {
    private final IamStore store;
    private final PermissionService permissionService;

    public PermissionController(IamStore store, PermissionService permissionService) {
        this.store = store;
        this.permissionService = permissionService;
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionRecord>> permissions(@RequestHeader("X-User-Id") Long actorUserId) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        permissionService.require(actor, "iam:permission:read");
        return ApiResponse.ok(List.copyOf(store.permissions()));
    }

    @GetMapping("/me/permissions")
    public ApiResponse<Set<String>> myPermissions(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(permissionService.effectivePermissions(SecurityContextUtil.requireUserId(actorUserId)));
    }
}
