package com.health.platform.oa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import com.health.platform.inventory.InventoryStockService;
import com.health.platform.inventory.ItemRecord;
import com.health.platform.inventory.ItemViewRecord;
import com.health.platform.notification.OaNotificationService;
import org.springframework.stereotype.Service;

@Service
public class ProcessRuntimeService {
    private final OaStore store;
    private final IamStore iamStore;
    private final PermissionService permissionService;
    private final InventoryStockService inventoryStockService;
    private final SupervisorResolver supervisorResolver;
    private final OaNotificationService notificationService;
    private final AuditService auditService;
    private final List<OaApprovalListener> approvalListeners;

    public ProcessRuntimeService(OaStore store, IamStore iamStore, PermissionService permissionService, InventoryStockService inventoryStockService, SupervisorResolver supervisorResolver, OaNotificationService notificationService, AuditService auditService, List<OaApprovalListener> approvalListeners) {
        this.store = store;
        this.iamStore = iamStore;
        this.permissionService = permissionService;
        this.inventoryStockService = inventoryStockService;
        this.supervisorResolver = supervisorResolver;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.approvalListeners = approvalListeners == null ? List.of() : approvalListeners;
    }

    public List<ProcessDefinitionRecord> definitions(long actorUserId) {
        permissionService.require(actorUserId, "oa:process:read");
        return List.copyOf(store.definitions());
    }

    public List<Map<String, Object>> startableProcesses(long actorUserId) {
        permissionService.require(actorUserId, "oa:instance:create");
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(startable("inbound_material", "inventory_inbound", "物资入库 OA", "MATERIAL_INBOUND"));
        result.add(startable("outbound_material", "inventory_outbound", "物品领用", "MATERIAL_OUTBOUND"));
        result.add(startable("reimbursement", "reimbursement", "报销 OA", "REIMBURSEMENT"));
        return result;
    }

    public ProcessInstanceRecord start(long actorUserId, String processCode, String businessType, String title, Map<String, Object> formData) {
        permissionService.require(actorUserId, "oa:instance:create");
        Map<String, Object> mutableForm = new LinkedHashMap<>(formData == null ? Map.of() : formData);
        validateStartForm(actorUserId, businessType, mutableForm);
        Object draft = mutableForm.remove("materialDraft");
        ProcessInstanceRecord instance = store.addInstance(processCode, businessType, title, actorUserId, mutableForm);
        if (draft instanceof OaMaterialDraftRecord materialDraft) {
            OaMaterialDraftRecord saved = store.addMaterialDraft(instance.id(), materialDraft);
            instance.formData().put("materialDraftId", saved.id());
        }
        ProcessNodeRecord firstNode = store.sortedNodes(instance.processDefinitionId()).stream().findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, "Process has no node"));
        enterNode(actorUserId, instance, firstNode);
        auditService.record(actorUserId, "oa.instance.create", "oa_process_instance", String.valueOf(instance.id()));
        return instance;
    }

    public OaMaterialDraftRecord createMaterialDraft(long actorUserId, long instanceId, OaMaterialDraftRecord draft) {
        permissionService.require(actorUserId, "oa:instance:create");
        mustInstance(instanceId);
        OaMaterialDraftRecord saved = store.addMaterialDraft(instanceId, draft);
        auditService.record(actorUserId, "oa.material_draft.create", "oa_form_material_draft", String.valueOf(saved.id()));
        return saved;
    }

    public List<ItemRecord> searchMaterials(long actorUserId, String keyword) {
        permissionService.requireAny(actorUserId, "inventory:item:read", "inventory:inbound:create", "inventory:outbound:create");
        return inventoryStockService.searchItemsForOa(actorUserId, keyword);
    }

    public List<ItemRecord> searchClaimableMaterials(long actorUserId, long warehouseId, String keyword) {
        permissionService.require(actorUserId, "inventory:outbound:create");
        return inventoryStockService.searchClaimableItems(actorUserId, warehouseId, keyword);
    }

    public List<ItemRecord> searchInboundMaterials(long actorUserId, long warehouseId, String keyword) {
        permissionService.require(actorUserId, "inventory:inbound:create");
        return inventoryStockService.searchInboundItems(actorUserId, warehouseId, keyword);
    }

    public ItemViewRecord itemView(long actorUserId, ItemRecord item) {
        return inventoryStockService.toItemView(actorUserId, item);
    }

    private Map<String, Object> startable(String processCode, String businessType, String title, String typeCode) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("processCode", processCode);
        item.put("businessType", businessType);
        item.put("title", title);
        item.put("typeCode", typeCode);
        return item;
    }

    @SuppressWarnings("unchecked")
    private void validateStartForm(long actorUserId, String businessType, Map<String, Object> formData) {
        if ("reimbursement".equals(businessType)) {
            Object vouchers = formData.get("voucherAttachmentIds");
            if (!(vouchers instanceof List<?> list) || list.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Reimbursement voucher is required");
            }
        }
        Map<String, Object> newMaterial = castMap(formData.get("newMaterial"));
        if (newMaterial != null && !newMaterial.isEmpty() && formData.get("itemId") == null) {
            if (permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN")) {
                ItemRecord item = inventoryStockService.createItem(actorUserId, new InventoryStockService.ItemRequest(
                    String.valueOf(newMaterial.getOrDefault("code", "OA-" + System.currentTimeMillis())),
                    String.valueOf(newMaterial.get("name")),
                    String.valueOf(newMaterial.getOrDefault("itemType", "non_medical")),
                    String.valueOf(newMaterial.get("unit")),
                    decimalValue(newMaterial, "defaultPrice", java.math.BigDecimal.ZERO)));
                formData.put("itemId", item.id());
            } else {
                OaMaterialDraftRecord draft = new OaMaterialDraftRecord(
                    0, 0,
                    String.valueOf(newMaterial.get("name")),
                    String.valueOf(newMaterial.getOrDefault("category", "")),
                    String.valueOf(newMaterial.getOrDefault("itemType", "non_medical")),
                    String.valueOf(newMaterial.getOrDefault("specification", "")),
                    String.valueOf(newMaterial.get("unit")),
                    decimalValue(newMaterial, "defaultPrice", java.math.BigDecimal.ZERO),
                    String.valueOf(newMaterial.getOrDefault("supplier", "")),
                    String.valueOf(newMaterial.getOrDefault("createReason", "")));
                Object imageIds = newMaterial.get("imageAttachmentIds");
                if (imageIds instanceof List<?> list) {
                    list.forEach(id -> draft.imageAttachmentIds().add(Long.parseLong(String.valueOf(id))));
                }
                formData.put("materialDraft", draft);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private java.math.BigDecimal decimalValue(Map<String, Object> form, String key, java.math.BigDecimal fallback) {
        Object value = form.get(key);
        if (value == null) return fallback;
        if (value instanceof java.math.BigDecimal decimal) return decimal;
        if (value instanceof Number number) return java.math.BigDecimal.valueOf(number.doubleValue());
        return new java.math.BigDecimal(String.valueOf(value));
    }

    public List<ProcessInstanceRecord> instances(long actorUserId) {
        permissionService.require(actorUserId, "oa:instance:read");
        return List.copyOf(store.instances());
    }

    public List<OaInstanceViewRecord> myInstances(long actorUserId) {
        permissionService.require(actorUserId, "oa:instance:read");
        return store.instances().stream()
            .filter(instance -> instance.initiatorUserId() == actorUserId)
            .map(this::toInstanceView)
            .toList();
    }

    private OaInstanceViewRecord toInstanceView(ProcessInstanceRecord instance) {
        OaTaskRecord pendingTask = store.tasks().stream()
            .filter(task -> task.processInstanceId() == instance.id() && task.status() == TaskStatus.PENDING)
            .findFirst()
            .orElse(null);
        String currentNodeName = pendingTask == null ? "" : store.findNode(pendingTask.nodeId()).map(ProcessNodeRecord::nodeName).orElse("");
        return new OaInstanceViewRecord(
            instance.id(),
            instance.processCode(),
            instance.businessType(),
            instance.title(),
            instance.status(),
            instance.initiatorUserId(),
            pendingTask == null ? null : pendingTask.id(),
            currentNodeName,
            currentHandler(pendingTask),
            stage(instance, pendingTask)
        );
    }

    private String currentHandler(OaTaskRecord task) {
        if (task == null) {
            return "-";
        }
        if (task.assigneeMode() == AssigneeMode.SUPERVISOR && task.resolvedSupervisorUserId() != null) {
            return iamStore.findUser(task.resolvedSupervisorUserId()).map(user -> user.displayName()).orElse("直属上级");
        }
        if (task.assigneeMode() == AssigneeMode.USER && task.assigneeUserId() != null) {
            return iamStore.findUser(task.assigneeUserId()).map(user -> user.displayName()).orElse("指定用户");
        }
        if (task.assigneeMode() == AssigneeMode.ROLE && task.assigneeRoleCode() != null) {
            return iamStore.findRole(task.assigneeRoleCode()).map(role -> role.name()).orElse(task.assigneeRoleCode());
        }
        if (task.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) {
            return iamStore.findUser(task.assigneeUserId()).map(user -> user.displayName()).orElse("起草人");
        }
        return task.assigneeMode().name();
    }

    private String stage(ProcessInstanceRecord instance, OaTaskRecord pendingTask) {
        if (instance.status() == InstanceStatus.APPROVED || instance.status() == InstanceStatus.REJECTED || instance.status() == InstanceStatus.CANCELLED) {
            return "结束";
        }
        if (pendingTask == null) {
            return "起草";
        }
        if (pendingTask.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) {
            return "起草";
        }
        return "审批";
    }

    public List<OaTaskRecord> todo(long actorUserId) {
        permissionService.require(actorUserId, "oa:task:read");
        List<OaTaskRecord> result = new ArrayList<>();
        for (OaTaskRecord task : store.tasks()) {
            if (task.status() == TaskStatus.PENDING && canHandle(actorUserId, task)) {
                result.add(task);
            }
        }
        return result;
    }

    public List<OaTaskRecord> handled(long actorUserId) {
        permissionService.require(actorUserId, "oa:task:read");
        return store.tasks().stream()
            .filter(task -> task.claimedByUserId() != null && task.claimedByUserId() == actorUserId)
            .filter(task -> task.status() != TaskStatus.PENDING)
            .toList();
    }

    public OaTaskDetailRecord taskDetail(long actorUserId, long taskId) {
        permissionService.require(actorUserId, "oa:task:read");
        OaTaskRecord task = store.findTask(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Task not found"));
        if (!canHandle(actorUserId, task)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user cannot view this task");
        }
        ProcessInstanceRecord instance = mustInstance(task.processInstanceId());
        ProcessNodeRecord node = store.findNode(task.nodeId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Process node not found"));
        String initiatorName = iamStore.findUser(instance.initiatorUserId())
            .map(user -> user.displayName())
            .orElse(String.valueOf(instance.initiatorUserId()));
        return new OaTaskDetailRecord(
            task.id(),
            instance.id(),
            instance.processCode(),
            instance.businessType(),
            instance.title(),
            instance.status(),
            instance.initiatorUserId(),
            initiatorName,
            task.nodeId(),
            node.nodeName(),
            task.assigneeMode(),
            task.status(),
            task.createdAt(),
            new LinkedHashMap<>(instance.formData()),
            displayData(instance.formData())
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> displayData(Map<String, Object> formData) {
        Map<String, Object> result = new LinkedHashMap<>();
        formData.forEach((key, value) -> {
            if ("warehouseId".equals(key) && value != null) {
                result.put("仓库", inventoryStockService.warehouseLabelForOa(longValue(value)));
            } else if ("itemId".equals(key) && value != null) {
                result.put("物资", inventoryStockService.itemLabelForOa(longValue(value)));
            } else if ("quantity".equals(key)) {
                result.put("数量", value);
            } else if ("unitPrice".equals(key)) {
                result.put("单价", value);
            } else if ("amount".equals(key)) {
                result.put("总价", value);
            } else if ("relatedInboundOrderId".equals(key)) {
                result.put("关联入库单", value);
            } else if ("newMaterial".equals(key) && value instanceof Map<?, ?> map) {
                result.put("新增物资", new LinkedHashMap<>((Map<String, Object>) map));
            } else if ("voucherAttachmentIds".equals(key)) {
                result.put("凭证附件", value);
            }
        });
        return result;
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public ProcessInstanceRecord approve(long actorUserId, long taskId, String comment) {
        OaTaskRecord task = mustPendingTask(taskId);
        requireTaskWritePermission(actorUserId, task);
        if (!canHandle(actorUserId, task)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user cannot approve this task");
        }
        task.approve(actorUserId);
        ProcessInstanceRecord instance = mustInstance(task.processInstanceId());
        if (task.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) {
            instance.markApproved();
            for (OaApprovalListener listener : approvalListeners) {
                listener.onApproved(instance);
            }
            auditService.record(actorUserId, "oa.task.confirm", "oa_task", String.valueOf(taskId));
            return instance;
        }
        advanceAfterApproval(actorUserId, instance, task.nodeId());
        auditService.record(actorUserId, "oa.task.approve", "oa_task", String.valueOf(taskId));
        return instance;
    }

    public ProcessInstanceRecord reject(long actorUserId, long taskId, String comment) {
        OaTaskRecord task = mustPendingTask(taskId);
        requireTaskWritePermission(actorUserId, task);
        if (!canHandle(actorUserId, task)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user cannot reject this task");
        }
        task.reject(actorUserId);
        ProcessInstanceRecord instance = mustInstance(task.processInstanceId());
        if (task.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) {
            instance.markRejected();
        } else {
            createDraftConfirmTask(instance);
        }
        auditService.record(actorUserId, "oa.task.reject", "oa_task", String.valueOf(taskId));
        return instance;
    }

    public OaTaskRecord urge(long actorUserId, long instanceId) {
        permissionService.require(actorUserId, "oa:task:urge");
        OaTaskRecord task = store.tasks().stream()
            .filter(t -> t.processInstanceId() == instanceId && t.status() == TaskStatus.PENDING)
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pending task not found"));
        task.incrementRemindCount();
        notificationService.manualUrge(task.id(), targetUserId(task));
        auditService.record(actorUserId, "oa.task.urge", "oa_process_instance", String.valueOf(instanceId));
        return task;
    }

    public ProcessInstanceRecord revoke(long actorUserId, long instanceId) {
        permissionService.require(actorUserId, "oa:instance:read");
        ProcessInstanceRecord instance = mustInstance(instanceId);
        if (instance.initiatorUserId() != actorUserId) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only initiator can revoke this process");
        }
        if (instance.status() != InstanceStatus.RUNNING && instance.status() != InstanceStatus.PENDING_CONFIG) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only running process can be revoked");
        }
        instance.markCancelled();
        store.tasks().stream()
            .filter(task -> task.processInstanceId() == instanceId && task.status() == TaskStatus.PENDING)
            .forEach(task -> task.cancel(actorUserId));
        auditService.record(actorUserId, "oa.instance.revoke", "oa_process_instance", String.valueOf(instanceId));
        return instance;
    }

    public boolean canHandle(long actorUserId, OaTaskRecord task) {
        return switch (task.assigneeMode()) {
            case USER, INITIATOR_SELECTED -> task.assigneeUserId() != null && task.assigneeUserId() == actorUserId;
            case ROLE -> task.assigneeRoleCode() != null && iamStore.hasRole(actorUserId, task.assigneeRoleCode());
            case SUPERVISOR -> task.resolvedSupervisorUserId() != null && task.resolvedSupervisorUserId() == actorUserId;
        };
    }

    private void createTask(ProcessInstanceRecord instance, ProcessNodeRecord node) {
        Long supervisor = null;
        if (node.assigneeMode() == AssigneeMode.SUPERVISOR) {
            supervisor = supervisorResolver.resolveDirectSupervisor(instance.initiatorUserId());
            if (supervisor == null) {
                instance.markPendingConfig();
                return;
            }
        }
        OaTaskRecord task = store.addTask(instance, node, supervisor);
        notificationService.taskArrived(task.id(), targetUserId(task));
    }

    private void enterNode(long actorUserId, ProcessInstanceRecord instance, ProcessNodeRecord node) {
        if (shouldAutoSkipMissingSupervisor(instance, node)) {
            OaTaskRecord skipped = store.addTask(instance, node, null);
            skipped.approve(instance.initiatorUserId());
            auditService.record(instance.initiatorUserId(), "oa.task.auto_skip_missing_supervisor", "oa_task", String.valueOf(skipped.id()));
            advanceAfterApproval(actorUserId, instance, node.id());
            return;
        }
        createTask(instance, node);
    }

    private void createDraftConfirmTask(ProcessInstanceRecord instance) {
        ProcessNodeRecord draftConfirm = store.sortedNodes(instance.processDefinitionId()).stream()
            .filter(node -> node.assigneeMode() == AssigneeMode.INITIATOR_SELECTED || "draft_confirm".equals(node.nodeCode()))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, "Draft confirmation node not found"));
        createTask(instance, draftConfirm);
    }

    private void advanceAfterApproval(long actorUserId, ProcessInstanceRecord instance, long currentNodeId) {
        long cursor = currentNodeId;
        while (true) {
            ProcessNodeRecord next = store.nextNode(instance, cursor).orElse(null);
            if (next == null) {
                createDraftConfirmTask(instance);
                return;
            }
            if (shouldAutoSkipMissingSupervisor(instance, next)) {
                OaTaskRecord skipped = store.addTask(instance, next, null);
                skipped.approve(instance.initiatorUserId());
                auditService.record(instance.initiatorUserId(), "oa.task.auto_skip_missing_supervisor", "oa_task", String.valueOf(skipped.id()));
                cursor = next.id();
                continue;
            }
            if (!isAutoSkippableSameAssignee(actorUserId, instance, next)) {
                createTask(instance, next);
                return;
            }
            OaTaskRecord skipped = createAutoSkippedTask(instance, next);
            skipped.approve(actorUserId);
            auditService.record(actorUserId, "oa.task.auto_skip", "oa_task", String.valueOf(skipped.id()));
            cursor = next.id();
        }
    }

    private OaTaskRecord createAutoSkippedTask(ProcessInstanceRecord instance, ProcessNodeRecord node) {
        Long supervisor = null;
        if (node.assigneeMode() == AssigneeMode.SUPERVISOR) {
            supervisor = supervisorResolver.resolveDirectSupervisor(instance.initiatorUserId());
            if (supervisor == null) {
                instance.markPendingConfig();
                throw new BusinessException(ErrorCode.INVALID_STATE, "Supervisor not configured");
            }
        }
        return store.addTask(instance, node, supervisor);
    }

    private boolean shouldAutoSkipMissingSupervisor(ProcessInstanceRecord instance, ProcessNodeRecord node) {
        if (node.assigneeMode() != AssigneeMode.SUPERVISOR) {
            return false;
        }
        return isOrganizationTopLevelAdministrator(instance.initiatorUserId());
    }

    private boolean isOrganizationTopLevelAdministrator(long userId) {
        return supervisorResolver.resolveDirectSupervisor(userId) == null
            && permissionService.hasPermission(userId, "iam:user:create")
            && permissionService.hasPermission(userId, "iam:role:write")
            && permissionService.hasPermission(userId, "iam:user-permission:write");
    }

    private boolean isAutoSkippableSameAssignee(long actorUserId, ProcessInstanceRecord instance, ProcessNodeRecord node) {
        return switch (node.assigneeMode()) {
            case USER -> node.assigneeUserId() != null && node.assigneeUserId() == actorUserId;
            case SUPERVISOR -> {
                Long supervisor = supervisorResolver.resolveDirectSupervisor(instance.initiatorUserId());
                yield supervisor != null && supervisor == actorUserId;
            }
            case ROLE -> node.assigneeRoleCode() != null && iamStore.hasRole(actorUserId, node.assigneeRoleCode());
            case INITIATOR_SELECTED -> false;
        };
    }

    private Long targetUserId(OaTaskRecord task) {
        if (task.assigneeMode() == AssigneeMode.USER) return task.assigneeUserId();
        if (task.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) return task.assigneeUserId();
        if (task.assigneeMode() == AssigneeMode.SUPERVISOR) return task.resolvedSupervisorUserId();
        return null;
    }

    private OaTaskRecord mustPendingTask(long taskId) {
        OaTaskRecord task = store.findTask(taskId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Task not found"));
        if (task.status() != TaskStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Task is not pending");
        }
        return task;
    }

    private void requireTaskWritePermission(long actorUserId, OaTaskRecord task) {
        if (task.assigneeMode() == AssigneeMode.INITIATOR_SELECTED && task.assigneeUserId() != null && task.assigneeUserId() == actorUserId) {
            permissionService.require(actorUserId, "oa:task:read");
            return;
        }
        permissionService.require(actorUserId, "oa:task:approve");
    }

    private ProcessInstanceRecord mustInstance(long instanceId) {
        return store.findInstance(instanceId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Instance not found"));
    }
}
