package com.health.platform.oa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class OaStore {
    private final AtomicLong definitionId = new AtomicLong(0);
    private final AtomicLong nodeId = new AtomicLong(0);
    private final AtomicLong instanceId = new AtomicLong(0);
    private final AtomicLong taskId = new AtomicLong(0);
    private final Map<Long, ProcessDefinitionRecord> definitions = new LinkedHashMap<>();
    private final Map<Long, ProcessInstanceRecord> instances = new LinkedHashMap<>();
    private final Map<Long, OaTaskRecord> tasks = new LinkedHashMap<>();

    public OaStore() {
        seedDefaults();
    }

    public Collection<ProcessDefinitionRecord> definitions() { return definitions.values(); }
    public Collection<ProcessInstanceRecord> instances() { return instances.values(); }
    public Collection<OaTaskRecord> tasks() { return tasks.values(); }
    public Optional<ProcessDefinitionRecord> findDefinitionByCode(String code) { return definitions.values().stream().filter(d -> d.code().equals(code) && d.enabled()).findFirst(); }
    public Optional<ProcessInstanceRecord> findInstance(long id) { return Optional.ofNullable(instances.get(id)); }
    public Optional<OaTaskRecord> findTask(long id) { return Optional.ofNullable(tasks.get(id)); }

    public ProcessInstanceRecord addInstance(String processCode, String businessType, String title, long initiatorUserId, Map<String, Object> formData) {
        ProcessDefinitionRecord definition = findDefinitionByCode(processCode).orElseThrow();
        ProcessInstanceRecord instance = new ProcessInstanceRecord(instanceId.incrementAndGet(), definition.id(), processCode, businessType, title, initiatorUserId, formData);
        instances.put(instance.id(), instance);
        return instance;
    }

    public OaTaskRecord addTask(ProcessInstanceRecord instance, ProcessNodeRecord node, Long resolvedSupervisorUserId) {
        OaTaskRecord task = new OaTaskRecord(taskId.incrementAndGet(), instance.id(), node.id(), node.assigneeMode(), node.assigneeUserId(), node.assigneeRoleCode(), resolvedSupervisorUserId);
        tasks.put(task.id(), task);
        instance.moveTo(node.id());
        return task;
    }

    public List<ProcessNodeRecord> sortedNodes(long definitionId) {
        return definitions.get(definitionId).nodes().stream().sorted(Comparator.comparingInt(ProcessNodeRecord::sortOrder)).toList();
    }

    public Optional<ProcessNodeRecord> findNode(long nodeId) {
        return definitions.values().stream().flatMap(d -> d.nodes().stream()).filter(n -> n.id() == nodeId).findFirst();
    }

    public Optional<ProcessNodeRecord> nextNode(ProcessInstanceRecord instance, long currentNodeId) {
        List<ProcessNodeRecord> nodes = sortedNodes(instance.processDefinitionId());
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id() == currentNodeId && i + 1 < nodes.size()) {
                return Optional.of(nodes.get(i + 1));
            }
        }
        return Optional.empty();
    }

    private void seedDefaults() {
        ProcessDefinitionRecord inbound = addDefinition("inbound_material", "物资入库 OA");
        addNode(inbound, "manager_approve", "汇报上级审批", 10, AssigneeMode.SUPERVISOR, null, null);
        addNode(inbound, "inventory_approve", "物资管理员审批", 20, AssigneeMode.ROLE, null, "INVENTORY_ADMIN");
        ProcessDefinitionRecord outbound = addDefinition("outbound_material", "物资出库 OA");
        addNode(outbound, "manager_approve", "汇报上级审批", 10, AssigneeMode.SUPERVISOR, null, null);
        addNode(outbound, "inventory_approve", "物资管理员审批", 20, AssigneeMode.ROLE, null, "INVENTORY_ADMIN");
        ProcessDefinitionRecord reimbursement = addDefinition("reimbursement", "报销 OA");
        addNode(reimbursement, "manager_approve", "汇报上级审批", 10, AssigneeMode.SUPERVISOR, null, null);
        addNode(reimbursement, "finance_approve", "财务审批", 20, AssigneeMode.ROLE, null, "FINANCE_APPROVER");
    }

    private ProcessDefinitionRecord addDefinition(String code, String name) {
        ProcessDefinitionRecord definition = new ProcessDefinitionRecord(definitionId.incrementAndGet(), code, name, 1);
        definitions.put(definition.id(), definition);
        return definition;
    }

    private void addNode(ProcessDefinitionRecord definition, String code, String name, int sortOrder, AssigneeMode mode, Long userId, String roleCode) {
        definition.nodes().add(new ProcessNodeRecord(nodeId.incrementAndGet(), definition.id(), code, name, "approval", sortOrder, mode, userId, roleCode, true, 1, 24));
    }
}
