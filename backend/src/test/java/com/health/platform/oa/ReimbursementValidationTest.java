package com.health.platform.oa;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import com.health.platform.iam.UserService;
import com.health.platform.inventory.InventoryStockService;
import com.health.platform.inventory.InventoryStore;
import com.health.platform.notification.OaNotificationMessageBuilder;
import com.health.platform.notification.OaNotificationRecipientResolver;
import com.health.platform.notification.OaNotificationService;
import com.health.platform.wecom.WeComMessageClient;
import com.health.platform.wecom.WeComProperties;
import com.health.platform.wecom.WeComTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReimbursementValidationTest {
    private ProcessRuntimeService service;

    @BeforeEach
    void setUp() {
        IamStore iamStore = new IamStore();
        PermissionService permissionService = new PermissionService(iamStore);
        AuditService auditService = new AuditService();
        UserService userService = new UserService(iamStore, permissionService, auditService);
        InventoryStockService inventoryStockService = new InventoryStockService(new InventoryStore(), permissionService, auditService, iamStore);
        OaStore oaStore = new OaStore();
        WeComProperties weComProperties = new WeComProperties();
        service = new ProcessRuntimeService(
            oaStore,
            iamStore,
            permissionService,
            inventoryStockService,
            new SupervisorResolver(userService),
            new OaNotificationService(
                new OaNotificationRecipientResolver(oaStore, iamStore),
                new OaNotificationMessageBuilder(oaStore),
                new WeComMessageClient(weComProperties, new WeComTokenService(weComProperties))),
            auditService,
            List.of(inventoryStockService));
    }

    @Test
    void reimbursementWithoutVoucherRejected() {
        Map<String, Object> form = new HashMap<>();
        form.put("amount", "100");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.start(3, "reimbursement", "reimbursement", "报销", form));
        assertTrue(ex.getMessage().contains("voucher"));
    }
}
