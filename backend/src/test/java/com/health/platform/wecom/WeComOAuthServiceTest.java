package com.health.platform.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.health.platform.common.BusinessException;
import com.health.platform.iam.IamStore;
import org.junit.jupiter.api.Test;

class WeComOAuthServiceTest {
    @Test
    void mockCodeLogsInBoundUserAndIssuesMobileToken() {
        IamStore iamStore = new IamStore();
        WeComProperties properties = new WeComProperties();
        WeComSessionService sessionService = new WeComSessionService(iamStore);
        WeComOAuthService service = new WeComOAuthService(properties, new WeComTokenService(properties), sessionService, iamStore);

        WeComOAuthService.MobileLoginResult result = service.login("mock:employee.wecom");

        assertEquals(3, result.userId());
        assertEquals("employee.wecom", result.wecomUserId());
        assertTrue(sessionService.resolveUserId("Bearer " + result.token()).isPresent());
    }

    @Test
    void unboundWecomUserCannotLogin() {
        IamStore iamStore = new IamStore();
        WeComProperties properties = new WeComProperties();
        WeComSessionService sessionService = new WeComSessionService(iamStore);
        WeComOAuthService service = new WeComOAuthService(properties, new WeComTokenService(properties), sessionService, iamStore);

        assertThrows(BusinessException.class, () -> service.login("mock:missing.wecom"));
    }
}
