package com.health.platform.iam;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
@RequestMapping("/api/v1/iam/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(userService.list(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserRecord> get(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId) {
        return ApiResponse.ok(userService.get(SecurityContextUtil.requireUserId(actorUserId), userId));
    }

    @PostMapping
    public ApiResponse<UserRecord> create(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody UserService.UserRequest request) {
        return ApiResponse.ok(userService.create(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserRecord> update(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId, @RequestBody UserService.UserRequest request) {
        return ApiResponse.ok(userService.update(SecurityContextUtil.requireUserId(actorUserId), userId, request));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<UserRecord> updateStatus(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId, @RequestBody Map<String, Boolean> request) {
        return ApiResponse.ok(userService.updateStatus(SecurityContextUtil.requireUserId(actorUserId), userId, Boolean.TRUE.equals(request.get("enabled"))));
    }

    @PutMapping("/{userId}/roles")
    public ApiResponse<UserRecord> assignRoles(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId, @RequestBody Set<String> roleCodes) {
        return ApiResponse.ok(userService.assignRoles(SecurityContextUtil.requireUserId(actorUserId), userId, roleCodes));
    }

    @PutMapping("/{userId}/permission-overrides")
    public ApiResponse<UserRecord> updateOverrides(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId, @RequestBody PermissionOverrideRequest request) {
        return ApiResponse.ok(userService.updatePermissionOverrides(
            SecurityContextUtil.requireUserId(actorUserId), userId, request.overrides(), request.reason()));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<UserRecord> resetPassword(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId) {
        return ApiResponse.ok(userService.resetPassword(SecurityContextUtil.requireUserId(actorUserId), userId));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId) {
        userService.delete(SecurityContextUtil.requireUserId(actorUserId), userId);
        return ApiResponse.ok(null);
    }

    public record PermissionOverrideRequest(Map<String, PermissionEffect> overrides, String reason) {
    }
}
