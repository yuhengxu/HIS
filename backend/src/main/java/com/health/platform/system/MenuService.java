package com.health.platform.system;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionEffect;
import com.health.platform.iam.PermissionService;
import com.health.platform.iam.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class MenuService {
    private final MenuStore store;
    private final IamStore iamStore;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public MenuService(MenuStore store, IamStore iamStore, PermissionService permissionService, AuditService auditService) {
        this.store = store;
        this.iamStore = iamStore;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    public List<MenuRecord> list(long actorUserId) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:read");
        return List.copyOf(store.menus());
    }

    public MenuRecord create(long actorUserId, CreateMenuRequest request) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:write");
        MenuRecord menu = store.create(request.code(), request.name(), request.parentId(), request.path(), request.icon(), request.sortOrder() == null ? 100 : request.sortOrder());
        auditService.record(actorUserId, "menu.create", "sys_menu", String.valueOf(menu.id()));
        return menu;
    }

    public MenuRecord update(long actorUserId, long menuId, MenuRecord.MenuRequest request) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:write");
        MenuRecord menu = store.find(menuId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Menu not found"));
        menu.update(request);
        auditService.record(actorUserId, "menu.update", "sys_menu", String.valueOf(menuId));
        return menu;
    }

    public void delete(long actorUserId, long menuId) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:write");
        store.delete(menuId);
        auditService.record(actorUserId, "menu.delete", "sys_menu", String.valueOf(menuId));
    }

    public Set<Long> roleMenus(long actorUserId, String roleCode) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:read");
        return store.roleMenuIds(roleCode);
    }

    public void saveRoleMenus(long actorUserId, String roleCode, Set<Long> menuIds) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:role-bind");
        store.saveRoleMenus(roleCode, menuIds);
        auditService.record(actorUserId, "menu.role.bind", "iam_role", roleCode);
    }

    public Map<Long, PermissionEffect> userMenuOverrides(long actorUserId, long userId) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:read");
        return store.userMenuOverrides(userId);
    }

    public void saveUserMenuOverrides(long actorUserId, long userId, Map<Long, PermissionEffect> overrides, String reason) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "menu:user-bind");
        Map<Long, PermissionEffect> map = store.userMenuOverrides(userId);
        map.clear();
        if (overrides != null) map.putAll(overrides);
        auditService.record(actorUserId, "menu.user.override", "iam_user", userId + ":" + (reason == null ? "" : reason));
    }

    public List<MenuStore.MenuTreeNode> currentUserMenus(long userId) {
        UserRecord user = iamStore.findUser(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        Set<Long> visible = new LinkedHashSet<>();
        for (String roleCode : user.roleCodes()) {
            visible.addAll(store.roleMenuIds(roleCode));
        }
        store.userMenuOverrides(userId).forEach((menuId, effect) -> {
            if (effect == PermissionEffect.GRANT) visible.add(menuId);
            if (effect == PermissionEffect.DENY) visible.remove(menuId);
        });
        return store.buildTree(visible);
    }

    public record CreateMenuRequest(String code, String name, Long parentId, String path, String icon, Integer sortOrder) {
    }
}
