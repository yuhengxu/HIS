package com.health.platform.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.health.platform.audit.AuditService;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionEffect;
import com.health.platform.iam.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuServiceTest {
    private MenuService menuService;
    private MenuStore menuStore;

    @BeforeEach
    void setUp() {
        menuStore = new MenuStore();
        IamStore iamStore = new IamStore();
        PermissionService permissionService = new PermissionService(iamStore);
        menuService = new MenuService(menuStore, iamStore, permissionService, new AuditService());
    }

    @Test
    void roleMenusVisibleForEmployee() {
        var menus = menuService.currentUserMenus(3);
        assertFalse(menus.isEmpty());
    }

    @Test
    void userMenuDenyOverridesRoleGrant() {
        long menuId = menuStore.menus().stream().filter(m -> "inventory.items".equals(m.code())).findFirst().orElseThrow().id();
        menuStore.userMenuOverrides(3).put(menuId, PermissionEffect.DENY);
        var menus = menuService.currentUserMenus(3);
        assertTrue(menus.stream().noneMatch(node -> "/inventory/items".equals(node.menu().path())));
    }
}
