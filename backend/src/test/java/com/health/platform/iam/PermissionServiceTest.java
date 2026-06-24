package com.health.platform.iam;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PermissionServiceTest {
    @Test
    void userPermissionOverrideGrantAndDenyWork() {
        IamStore store = new IamStore();
        PermissionService service = new PermissionService(store);
        UserRecord employee = store.findUser(3).orElseThrow();

        assertFalse(service.hasPermission(3, "oa:task:approve"));
        employee.permissionOverrides().put("oa:task:approve", PermissionEffect.GRANT);
        assertTrue(service.hasPermission(3, "oa:task:approve"));
        employee.permissionOverrides().put("oa:task:approve", PermissionEffect.DENY);
        assertFalse(service.hasPermission(3, "oa:task:approve"));
    }
}
