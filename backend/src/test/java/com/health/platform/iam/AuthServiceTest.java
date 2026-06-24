package com.health.platform.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    @Test
    void adminAndMemberInitialPasswordsWork() {
        IamStore store = new IamStore();
        AuthService authService = new AuthService(store, new PermissionService(store), new AuditService());

        assertEquals(1, authService.login(new AuthService.LoginRequest("admin", "1qaz@WSX")).userId());
        assertEquals(3, authService.login(new AuthService.LoginRequest("employee", "qwer1234")).userId());
        assertThrows(BusinessException.class, () -> authService.login(new AuthService.LoginRequest("admin", "qwer1234")));
    }
}
