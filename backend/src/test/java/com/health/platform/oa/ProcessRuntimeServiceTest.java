package com.health.platform.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void inboundApprovalReturnsToDraftConfirmBeforeIncreasingStock() {
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

        assertEquals(InstanceStatus.RUNNING, instance.status());
        assertTrue(fixture.inventoryStore.stockRecords().stream().noneMatch(s -> s.ownerUserId() == 3 && s.warehouseId() == 1 && s.itemId() == 1 && s.quantity().compareTo(new BigDecimal("5")) == 0));
        OaTaskRecord draftConfirmTask = fixture.runtime.todo(3).get(0);
        assertEquals(AssigneeMode.INITIATOR_SELECTED, draftConfirmTask.assigneeMode());
        fixture.runtime.approve(3, draftConfirmTask.id(), "确认完成");

        assertEquals(InstanceStatus.APPROVED, instance.status());
        assertTrue(fixture.inventoryStore.stockRecords().stream().anyMatch(s -> s.ownerUserId() == 3 && s.warehouseId() == 1 && s.itemId() == 1 && s.quantity().compareTo(new BigDecimal("5")) == 0));
    }

    @Test
    void rejectedApprovalReturnsToDraftConfirmThenCanEndRejected() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(3, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 5,
            "unitPrice", "0.60"
        ));

        OaTaskRecord managerTask = fixture.runtime.todo(2).get(0);
        fixture.runtime.reject(2, managerTask.id(), "不同意");

        assertEquals(InstanceStatus.RUNNING, instance.status());
        OaTaskRecord draftConfirmTask = fixture.runtime.todo(3).get(0);
        assertEquals(AssigneeMode.INITIATOR_SELECTED, draftConfirmTask.assigneeMode());
        fixture.runtime.reject(3, draftConfirmTask.id(), "确认退回");

        assertEquals(InstanceStatus.REJECTED, instance.status());
        assertTrue(fixture.inventoryStore.stockRecords().stream().noneMatch(s -> s.ownerUserId() == 3 && s.warehouseId() == 1 && s.itemId() == 1));
    }

    @Test
    void employeeCanLoadWorkspaceAfterStartingInbound() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(3, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 2,
            "unitPrice", "0.60"
        ));

        assertTrue(fixture.runtime.todo(3).isEmpty());
        assertEquals(1, fixture.runtime.myInstances(3).size());
        assertEquals(instance.id(), fixture.runtime.myInstances(3).get(0).id());
    }

    @Test
    void initiatorCanRevokeRunningProcessAndRemovePendingTodo() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(3, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 2,
            "unitPrice", "0.60"
        ));
        assertEquals(1, fixture.runtime.todo(2).size());

        fixture.runtime.revoke(3, instance.id());

        assertEquals(InstanceStatus.CANCELLED, instance.status());
        assertTrue(fixture.runtime.todo(2).isEmpty());
    }

    @Test
    void topLevelAdministratorSkipsMissingSupervisorAndContinuesToNextNode() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(1, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 2,
            "unitPrice", "0.60"
        ));

        assertEquals(InstanceStatus.RUNNING, instance.status());
        assertEquals(1, fixture.runtime.handled(1).stream()
            .filter(task -> task.assigneeMode() == AssigneeMode.SUPERVISOR)
            .count());
        assertEquals(1, fixture.runtime.todo(4).size());
    }

    @Test
    void nonIamAdministratorWithoutSupervisorStillRequiresConfiguration() {
        Fixture fixture = new Fixture();
        ProcessInstanceRecord instance = fixture.runtime.start(4, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 2,
            "unitPrice", "0.60"
        ));

        assertEquals(InstanceStatus.PENDING_CONFIG, instance.status());
        assertTrue(fixture.runtime.todo(4).isEmpty());
    }

    @Test
    void managerCanOpenTodoDetailButInitiatorCannotOpenManagersTask() {
        Fixture fixture = new Fixture();
        fixture.runtime.start(3, "inbound_material", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 2,
            "unitPrice", "0.60",
            "amount", "1.20"
        ));
        OaTaskRecord managerTask = fixture.runtime.todo(2).get(0);

        OaTaskDetailRecord detail = fixture.runtime.taskDetail(2, managerTask.id());

        assertEquals("入库", detail.title());
        assertEquals("普通员工", detail.initiatorName());
        assertEquals("医用口罩 (MASK)", detail.displayData().get("物资"));
        assertThrows(RuntimeException.class, () -> fixture.runtime.taskDetail(3, managerTask.id()));
    }

    @Test
    void consecutiveSameAssigneeNodesAreAutoSkippedAndKeptAsHandled() {
        Fixture fixture = new Fixture();
        ProcessDefinitionRecord definition = fixture.oaStore.createDefinition("same_user_inbound", "同人审批入库", "inventory_inbound", "测试", 1);
        fixture.oaStore.saveNodes(definition.id(), List.of(
            new ProcessNodeRecord.NodeRequest("manager_one", "负责人一审", "APPROVAL", 10, AssigneeMode.USER, 2L, null, null, null, true, 1, 24),
            new ProcessNodeRecord.NodeRequest("manager_two", "负责人二审", "APPROVAL", 20, AssigneeMode.USER, 2L, null, null, null, true, 1, 24)
        ));
        definition.markPublished(1);
        ProcessInstanceRecord instance = fixture.runtime.start(3, "same_user_inbound", "inventory_inbound", "入库", Map.of(
            "warehouseId", 1,
            "itemId", 1,
            "quantity", 2,
            "unitPrice", "0.60"
        ));

        OaTaskRecord first = fixture.runtime.todo(2).get(0);
        fixture.runtime.approve(2, first.id(), "同意");

        assertTrue(fixture.runtime.todo(2).isEmpty());
        assertEquals(2, fixture.runtime.handled(2).size());
        assertEquals(AssigneeMode.INITIATOR_SELECTED, fixture.runtime.todo(3).get(0).assigneeMode());
        assertEquals(InstanceStatus.RUNNING, instance.status());
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
        final InventoryStockService inventoryService = new InventoryStockService(inventoryStore, permissionService, auditService, iamStore);
        final ProcessRuntimeService runtime = new ProcessRuntimeService(oaStore, iamStore, permissionService, inventoryService, supervisorResolver, notificationService, auditService, List.of(inventoryService));
    }
}
