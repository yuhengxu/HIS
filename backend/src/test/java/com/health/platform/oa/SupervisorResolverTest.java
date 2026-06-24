package com.health.platform.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.health.platform.audit.AuditService;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import com.health.platform.iam.UserService;
import org.junit.jupiter.api.Test;

class SupervisorResolverTest {
    @Test
    void resolvesDirectSupervisor() {
        IamStore store = new IamStore();
        PermissionService permissionService = new PermissionService(store);
        UserService userService = new UserService(store, permissionService, new AuditService());
        SupervisorResolver resolver = new SupervisorResolver(userService);

        assertEquals(2L, resolver.resolveDirectSupervisor(3));
    }
}
