package com.health.platform.iam;

import java.util.List;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/iam/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(roleService.list(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/{code}")
    public ApiResponse<RoleRecord> get(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable String code) {
        return ApiResponse.ok(roleService.get(SecurityContextUtil.requireUserId(actorUserId), code));
    }

    @PostMapping
    public ApiResponse<RoleRecord> create(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody RoleService.RoleRequest request) {
        return ApiResponse.ok(roleService.create(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @PutMapping("/{code}")
    public ApiResponse<RoleRecord> update(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable String code, @RequestBody RoleService.RoleRequest request) {
        return ApiResponse.ok(roleService.update(SecurityContextUtil.requireUserId(actorUserId), code, request));
    }

    @DeleteMapping("/{code}")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable String code) {
        roleService.delete(SecurityContextUtil.requireUserId(actorUserId), code);
        return ApiResponse.ok(null);
    }
}
