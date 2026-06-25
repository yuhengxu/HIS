package com.health.platform.oa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class OaStore {
    private final AtomicLong definitionId = new AtomicLong(0);
    private final AtomicLong nodeId = new AtomicLong(0);
    private final AtomicLong instanceId = new AtomicLong(0);
    private final AtomicLong taskId = new AtomicLong(0);
    private final AtomicLong materialDraftId = new AtomicLong(0);
    private final Map<Long, ProcessDefinitionRecord> definitions = new LinkedHashMap<>();
    private final Map<Long, ProcessInstanceRecord> instances = new LinkedHashMap<>();
    private final Map<Long, OaTaskRecord> tasks = new LinkedHashMap<>();
    private final Map<Long, OaMaterialDraftRecord> materialDrafts = new LinkedHashMap<>();

    public OaStore() {
        seedDefaults();
    }

    public Collection<ProcessDefinitionRecord> definitions() {
        return definitions.values().stream().filter(d -> d.deletedAt() == null).toList();
    }

    public Collection<ProcessInstanceRecord> instances() { return instances.values(); }
    public Collection<OaTaskRecord> tasks() { return tasks.values(); }

    public Optional<ProcessDefinitionRecord> findDefinition(long id) {
        return Optional.ofNullable(definitions.get(id)).filter(d -> d.deletedAt() == null);
    }

    public Optional<ProcessDefinitionRecord> findDefinitionByCode(String code) {
        return definitions.values().stream()
            .filter(d -> d.deletedAt() == null && d.code().equals(code) && d.enabled() && "enabled".equals(d.status()))
            .max(Comparator.comparingInt(ProcessDefinitionRecord::version));
    }

    public Optional<ProcessDefinitionRecord> findLatestByCode(String code) {
        return definitions.values().stream()
            .filter(d -> d.deletedAt() == null && d.code().equals(code))
            .max(Comparator.comparingInt(ProcessDefinitionRecord::version));
    }

    public long countInstancesByDefinition(long definitionId) {
        return instances.values().stream().filter(i -> i.processDefinitionId() == definitionId).count();
    }

    public int nextVersion(String code) {
        return definitions.values().stream()
            .filter(d -> d.code().equals(code))
            .mapToInt(ProcessDefinitionRecord::version)
            .max()
            .orElse(0) + 1;
    }

    public ProcessDefinitionRecord createDefinition(String code, String name, String businessType, String description, long actorUserId) {
        if (findLatestByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Process code already exists");
        }
        ProcessDefinitionRecord definition = new ProcessDefinitionRecord(definitionId.incrementAndGet(), code, name, 1);
        definition.markDraft();
        definition.setCreatedBy(actorUserId);
        definition.setUpdatedBy(actorUserId);
        definition.updateBasic(name, description, businessType, Map.of(), null);
        definitions.put(definition.id(), definition);
        return definition;
    }

    public ProcessDefinitionRecord copyDefinition(long sourceId, String newCode, String newName, long actorUserId) {
        ProcessDefinitionRecord source = findDefinition(sourceId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Process not found"));
        ProcessDefinitionRecord copy = createDefinition(newCode, newName, source.businessType(), source.description(), actorUserId);
        copy.updateBasic(newName, source.description(), source.businessType(), source.formSchema(), null);
        for (ProcessNodeRecord node : sortedNodes(source.id())) {
            copy.nodes().add(cloneNode(copy.id(), node));
        }
        return copy;
    }

    public ProcessDefinitionRecord newVersionFrom(long sourceId, long actorUserId) {
        ProcessDefinitionRecord source = findDefinition(sourceId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Process not found"));
        int version = nextVersion(source.code());
        ProcessDefinitionRecord next = new ProcessDefinitionRecord(definitionId.incrementAndGet(), source.code(), source.name(), version);
        next.markDraft();
        next.setCreatedBy(actorUserId);
        next.setUpdatedBy(actorUserId);
        next.updateBasic(source.name(), source.description(), source.businessType(), source.formSchema(), null);
        for (ProcessNodeRecord node : sortedNodes(source.id())) {
            next.nodes().add(cloneNode(next.id(), node));
        }
        definitions.put(next.id(), next);
        return next;
    }

    public void saveNodes(long definitionId, List<ProcessNodeRecord.NodeRequest> nodeRequests) {
        ProcessDefinitionRecord definition = findDefinition(definitionId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Process not found"));
        definition.nodes().clear();
        int order = 10;
        for (ProcessNodeRecord.NodeRequest request : nodeRequests) {
            ProcessNodeRecord node = new ProcessNodeRecord(
                nodeId.incrementAndGet(), definitionId,
                request.nodeCode() == null ? "node_" + order : request.nodeCode(),
                request.nodeName() == null ? "审批节点" : request.nodeName(),
                request.nodeType() == null ? "APPROVAL" : request.nodeType(),
                request.sortOrder() == null ? order : request.sortOrder(),
                request.assigneeMode() == null ? AssigneeMode.ROLE : request.assigneeMode(),
                request.assigneeUserId(), request.assigneeRoleCode(),
                request.requireSelfOnly() == null || request.requireSelfOnly(),
                request.supervisorLevel() == null ? 1 : request.supervisorLevel(),
                request.timeoutHours() == null ? 24 : request.timeoutHours());
            if (request.approvePolicy() != null) node.updateFrom(request);
            definition.nodes().add(node);
            order += 10;
        }
        boolean hasDraftConfirm = definition.nodes().stream()
            .anyMatch(node -> node.assigneeMode() == AssigneeMode.INITIATOR_SELECTED || "draft_confirm".equals(node.nodeCode()));
        if (!hasDraftConfirm) {
            addNode(definition, "draft_confirm", "起草人确认", order, AssigneeMode.INITIATOR_SELECTED, null, null);
        }
    }

    public Optional<ProcessInstanceRecord> findInstance(long id) { return Optional.ofNullable(instances.get(id)); }
    public Optional<OaTaskRecord> findTask(long id) { return Optional.ofNullable(tasks.get(id)); }

    public OaMaterialDraftRecord addMaterialDraft(long instanceId, OaMaterialDraftRecord draft) {
        OaMaterialDraftRecord saved = new OaMaterialDraftRecord(
            materialDraftId.incrementAndGet(), instanceId, draft.name(), draft.category(), draft.itemType(),
            draft.specification(), draft.unit(), draft.defaultPrice(), draft.supplier(), draft.createReason());
        saved.imageAttachmentIds().addAll(draft.imageAttachmentIds());
        materialDrafts.put(saved.id(), saved);
        return saved;
    }

    public List<OaMaterialDraftRecord> materialDraftsByInstance(long instanceId) {
        return materialDrafts.values().stream().filter(d -> d.instanceId() == instanceId).toList();
    }

    public ProcessInstanceRecord addInstance(String processCode, String businessType, String title, long initiatorUserId, Map<String, Object> formData) {
        ProcessDefinitionRecord definition = findDefinitionByCode(processCode).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Process definition not found"));
        ProcessInstanceRecord instance = new ProcessInstanceRecord(instanceId.incrementAndGet(), definition.id(), processCode, businessType, title, initiatorUserId, formData);
        instances.put(instance.id(), instance);
        return instance;
    }

    public OaTaskRecord addTask(ProcessInstanceRecord instance, ProcessNodeRecord node, Long resolvedSupervisorUserId) {
        Long assigneeUserId = node.assigneeUserId();
        if (node.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) {
            assigneeUserId = Long.valueOf(instance.initiatorUserId());
        }
        OaTaskRecord task = new OaTaskRecord(taskId.incrementAndGet(), instance.id(), node.id(), node.assigneeMode(), assigneeUserId, node.assigneeRoleCode(), resolvedSupervisorUserId);
        tasks.put(task.id(), task);
        instance.moveTo(node.id());
        return task;
    }

    public List<ProcessNodeRecord> sortedNodes(long definitionId) {
        ProcessDefinitionRecord definition = definitions.get(definitionId);
        if (definition == null) return List.of();
        return definition.nodes().stream().sorted(Comparator.comparingInt(ProcessNodeRecord::sortOrder)).toList();
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

    private ProcessNodeRecord cloneNode(long definitionId, ProcessNodeRecord source) {
        ProcessNodeRecord node = new ProcessNodeRecord(
            nodeId.incrementAndGet(), definitionId, source.nodeCode(), source.nodeName(), source.nodeType(),
            source.sortOrder(), source.assigneeMode(), source.assigneeUserId(), source.assigneeRoleCode(),
            source.requireSelfOnly(), source.supervisorLevel(), source.timeoutHours());
        node.updateFrom(new ProcessNodeRecord.NodeRequest(
            null, null, null, null, null, null, null, source.approvePolicy(), source.rejectPolicy(), null, null, null));
        return node;
    }

    private void seedDefaults() {
        ProcessDefinitionRecord inbound = addBuiltinDefinition("inbound_material", "物资入库 OA", "inventory_inbound");
        addNode(inbound, "manager_approve", "汇报上级审批", 10, AssigneeMode.SUPERVISOR, null, null);
        addNode(inbound, "inventory_approve", "物资管理员审批", 20, AssigneeMode.ROLE, null, "INVENTORY_ADMIN");
        addNode(inbound, "draft_confirm", "起草人确认", 30, AssigneeMode.INITIATOR_SELECTED, null, null);
        ProcessDefinitionRecord outbound = addBuiltinDefinition("outbound_material", "物品领用", "inventory_outbound");
        addNode(outbound, "manager_approve", "汇报上级审批", 10, AssigneeMode.SUPERVISOR, null, null);
        addNode(outbound, "inventory_approve", "物资管理员审批", 20, AssigneeMode.ROLE, null, "INVENTORY_ADMIN");
        addNode(outbound, "draft_confirm", "起草人确认", 30, AssigneeMode.INITIATOR_SELECTED, null, null);
        ProcessDefinitionRecord reimbursement = addBuiltinDefinition("reimbursement", "报销 OA", "reimbursement");
        addNode(reimbursement, "manager_approve", "汇报上级审批", 10, AssigneeMode.SUPERVISOR, null, null);
        addNode(reimbursement, "finance_approve", "财务审批", 20, AssigneeMode.ROLE, null, "FINANCE_APPROVER");
        addNode(reimbursement, "draft_confirm", "起草人确认", 30, AssigneeMode.INITIATOR_SELECTED, null, null);
    }

    private ProcessDefinitionRecord addBuiltinDefinition(String code, String name, String businessType) {
        ProcessDefinitionRecord definition = new ProcessDefinitionRecord(definitionId.incrementAndGet(), code, name, 1);
        definition.setBuiltin(true);
        definition.markPublished(1);
        definition.updateBasic(name, "系统内置流程", businessType, Map.of(), true);
        definitions.put(definition.id(), definition);
        return definition;
    }

    private void addNode(ProcessDefinitionRecord definition, String code, String name, int sortOrder, AssigneeMode mode, Long userId, String roleCode) {
        definition.nodes().add(new ProcessNodeRecord(nodeId.incrementAndGet(), definition.id(), code, name, "APPROVAL", sortOrder, mode, userId, roleCode, true, 1, 24));
    }
}
