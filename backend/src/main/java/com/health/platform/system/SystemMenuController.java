package com.health.platform.system;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import com.health.platform.iam.PermissionEffect;
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
@RequestMapping("/api/v1/system/menus")
public class SystemMenuController {
    private final MenuService menuService;

    public SystemMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ApiResponse<List<MenuRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuService.list(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @PostMapping
    public ApiResponse<MenuRecord> create(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody MenuService.CreateMenuRequest request) {
        return ApiResponse.ok(menuService.create(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @PutMapping("/{menuId}")
    public ApiResponse<MenuRecord> update(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long menuId, @RequestBody MenuRecord.MenuRequest request) {
        return ApiResponse.ok(menuService.update(SecurityContextUtil.requireUserId(actorUserId), menuId, request));
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long menuId) {
        menuService.delete(SecurityContextUtil.requireUserId(actorUserId), menuId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/roles/{roleCode}")
    public ApiResponse<Set<Long>> roleMenus(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable String roleCode) {
        return ApiResponse.ok(menuService.roleMenus(SecurityContextUtil.requireUserId(actorUserId), roleCode));
    }

    @PutMapping("/roles/{roleCode}")
    public ApiResponse<Void> saveRoleMenus(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable String roleCode, @RequestBody Set<Long> menuIds) {
        menuService.saveRoleMenus(SecurityContextUtil.requireUserId(actorUserId), roleCode, menuIds);
        return ApiResponse.ok(null);
    }

    @GetMapping("/users/{userId}/overrides")
    public ApiResponse<Map<Long, PermissionEffect>> userOverrides(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId) {
        return ApiResponse.ok(menuService.userMenuOverrides(SecurityContextUtil.requireUserId(actorUserId), userId));
    }

    @PutMapping("/users/{userId}/overrides")
    public ApiResponse<Void> saveUserOverrides(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long userId, @RequestBody UserMenuOverrideRequest request) {
        menuService.saveUserMenuOverrides(SecurityContextUtil.requireUserId(actorUserId), userId, request.overrides(), request.reason());
        return ApiResponse.ok(null);
    }

    public record UserMenuOverrideRequest(Map<Long, PermissionEffect> overrides, String reason) {
    }
}
