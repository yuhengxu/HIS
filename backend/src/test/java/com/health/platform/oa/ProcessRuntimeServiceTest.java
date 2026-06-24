package com.health.platform.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import com.health.platform.iam.UserService;
import com.health.platform.inventory.InventoryStockService;
import com.health.platform.inventory.InventoryStore;
import com.health.platform.notification.OaNotificationService;
import org.junit.jupiter.api.Test;

class ProcessRuntimeServiceTest {
    @Test
    void inboundApprovalFlowsThroughSupervisorAndRoleThenIncreasesStock() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(3, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 5,
            "unitPrice", "0.60"
        ));

        assertEquals(InstanceStatus.RUNNING, instance.status());
        OaTaskRecord managerTask = fixture.runtime.todo(2).get(0);
        fixture.runtime.approve(2, managerTask.id(), "同意");
        OaTaskRecord inventoryTask = fixture.runtime.todo(4).get(0);
        fixture.runtime.approve(4, inventoryTask.id(), "入库");

        assertEquals(InstanceStatus.APPROVED, instance.status());
        assertTrue(fixture.inventoryStore.stockRecords().stream().anyMatch(s -> s.warehouseId() == 1 && s.itemId() == 1 && s.quantity().compareTo(new BigDecimal("105")) == 0));
    }

    @Test
    void urgeCreatesNotificationLog() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(3, "inbound_material", "inventory_inbound", "入库", Map.of());
        fixture.runtime.urge(3, instance.id());
        assertTrue(fixture.notificationService.logs().stream().anyMatch(log -> "OA_TASK_MANUAL_URGE".equals(log.messageType())));
    }

    static class Fixture {
        final IamStore iamStore = new IamStore();
        final PermissionService permissionService = new PermissionService(iamStore);
        final AuditService auditService = new AuditService();
        final UserService userService = new UserService(iamStore, permissionService, auditService);
        final SupervisorResolver supervisorResolver = new SupervisorResolver(userService);
        final OaStore oaStore = new OaStore();
        final OaNotificationService notificationService = new OaNotificationService();
        final InventoryStore inventoryStore = new InventoryStore();
        final InventoryStockService inventoryService = new InventoryStockService(inventoryStore, permissionService, auditService);
        final ProcessRuntimeService runtime = new ProcessRuntimeService(oaStore, iamStore, permissionService, inventoryService, supervisorResolver, notificationService, auditService, List.of(inventoryService));
    }
}
