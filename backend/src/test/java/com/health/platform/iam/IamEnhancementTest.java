package com.health.platform.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IamEnhancementTest {
    private IamStore store;
    private PermissionService permissionService;
    private UserService userService;
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        store = new IamStore();
        permissionService = new PermissionService(store);
        AuditService auditService = new AuditService();
        userService = new UserService(store, permissionService, auditService);
        roleService = new RoleService(store, permissionService, auditService);
    }

    @Test
    void nonSystemAdminCannotListUsers() {
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.list(3));
        assertTrue(ex.getMessage().contains("SYSTEM_ADMIN"));
    }

    @Test
    void adminCreatesUserWithDefaultPasswordHash() {
        UserRecord created = userService.create(1, new UserService.UserRequest(
            "tester", "测试员", null, null, "信息科", null, true));
        assertEquals("tester", created.username());
        assertTrue(created.mustChangePassword());
        assertTrue(store.findUserByUsername("tester").isPresent());
        assertEquals(PasswordHasher.sha256("qwer1234"), created.passwordHash());
    }

    @Test
    void duplicateUsernameRejected() {
        userService.create(1, new UserService.UserRequest("dup", "重复", null, null, null, null, true));
        assertThrows(BusinessException.class, () -> userService.create(1, new UserService.UserRequest("dup", "重复2", null, null, null, null, true)));
    }

    @Test
    void systemRoleCannotBeDeleted() {
        assertThrows(BusinessException.class, () -> roleService.delete(1, "SYSTEM_ADMIN"));
    }

    @Test
    void permissionDescriptionsArePresent() {
        PermissionRecord permission = store.permissions().stream()
            .filter(p -> "iam:user:create".equals(p.code()))
            .findFirst()
            .orElseThrow();
        assertFalse(permission.description().isBlank());
        assertEquals("IAM", permission.domain());
    }
}
