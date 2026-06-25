package com.health.platform.oa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OaProcessDefinitionServiceTest {
    private OaProcessDefinitionService service;
    private OaStore store;

    @BeforeEach
    void setUp() {
        store = new OaStore();
        IamStore iamStore = new IamStore();
        PermissionService permissionService = new PermissionService(iamStore);
        service = new OaProcessDefinitionService(store, iamStore, permissionService, new AuditService());
    }

    @Test
    void createAndPublishProcessDefinition() {
        ProcessDefinitionRecord created = service.create(1, new OaProcessDefinitionService.DefinitionRequest(
            "custom_flow", "自定义流程", "custom", "测试", null, null,
            List.of(new ProcessNodeRecord.NodeRequest("n1", "审批", "APPROVAL", 10, AssigneeMode.ROLE, null, "OA_ADMIN", "ANY_ONE", "BACK_TO_START", true, 1, 24))));
        ProcessDefinitionRecord published = service.publish(1, created.id());
        assertEquals("enabled", published.status());
    }

    @Test
    void saveNodesCreatesNewVersionWhenInstancesExist() {
        ProcessDefinitionRecord definition = store.definitions().stream().filter(d -> "inbound_material".equals(d.code())).findFirst().orElseThrow();
        store.addInstance("inbound_material", "inventory_inbound", "测试", 3, java.util.Map.of());
        List<ProcessNodeRecord.NodeRequest> nodes = List.of(
            new ProcessNodeRecord.NodeRequest("manager_approve", "汇报上级审批", "APPROVAL", 10, AssigneeMode.SUPERVISOR, null, null, "ANY_ONE", "BACK_TO_START", true, 1, 24),
            new ProcessNodeRecord.NodeRequest("inventory_approve", "物资管理员审批", "APPROVAL", 20, AssigneeMode.ROLE, null, "INVENTORY_ADMIN", "ANY_ONE", "BACK_TO_START", true, 1, 24));
        ProcessDefinitionRecord next = service.saveNodes(1, definition.id(), nodes);
        assertTrue(next.version() > definition.version());
    }

    @Test
    void employeeCannotCreateProcessDefinition() {
        assertThrows(BusinessException.class, () -> service.create(3,
            new OaProcessDefinitionService.DefinitionRequest("x", "x", "x", "", null, null, null)));
    }
}
