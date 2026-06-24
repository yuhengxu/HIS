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
        result.add(startable("outbound_material", "inventory_outbound", "物资出库 OA", "MATERIAL_OUTBOUND"));
        result.add(startable("reimbursement", "reimbursement", "报销 OA", "REIMBURSEMENT"));
        return result;
    }

    public ProcessInstanceRecord start(long actorUserId, String processCode, String businessType, String title, Map<String, Object> formData) {
        permissionService.require(actorUserId, "oa:instance:create");
        Map<String, Object> mutableForm = new LinkedHashMap<>(formData == null ? Map.of() : formData);
        validateStartForm(actorUserId, businessType, mutableForm);
        ProcessInstanceRecord instance = store.addInstance(processCode, businessType, title, actorUserId, mutableForm);
        Object draft = mutableForm.remove("materialDraft");
        if (draft instanceof OaMaterialDraftRecord materialDraft) {
            store.addMaterialDraft(instance.id(), materialDraft);
            mutableForm.put("materialDraftId", materialDraft.id());
        }
        ProcessNodeRecord firstNode = store.sortedNodes(instance.processDefinitionId()).stream().findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, "Process has no node"));
        createTask(instance, firstNode);
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
        permissionService.require(actorUserId, "inventory:item:read");
        return inventoryStockService.searchItems(actorUserId, keyword);
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

    public ProcessInstanceRecord approve(long actorUserId, long taskId, String comment) {
        permissionService.require(actorUserId, "oa:task:approve");
        OaTaskRecord task = mustPendingTask(taskId);
        if (!canHandle(actorUserId, task)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user cannot approve this task");
        }
        task.approve(actorUserId);
        ProcessInstanceRecord instance = mustInstance(task.processInstanceId());
        store.nextNode(instance, task.nodeId()).ifPresentOrElse(
            next -> createTask(instance, next),
            () -> {
                instance.markApproved();
                for (OaApprovalListener listener : approvalListeners) {
                    listener.onApproved(instance);
                }
            }
        );
        auditService.record(actorUserId, "oa.task.approve", "oa_task", String.valueOf(taskId));
        return instance;
    }

    public ProcessInstanceRecord reject(long actorUserId, long taskId, String comment) {
        permissionService.require(actorUserId, "oa:task:approve");
        OaTaskRecord task = mustPendingTask(taskId);
        if (!canHandle(actorUserId, task)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user cannot reject this task");
        }
        task.reject(actorUserId);
        ProcessInstanceRecord instance = mustInstance(task.processInstanceId());
        instance.markRejected();
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

    public boolean canHandle(long actorUserId, OaTaskRecord task) {
        return switch (task.assigneeMode()) {
            case USER -> task.assigneeUserId() != null && task.assigneeUserId() == actorUserId;
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

    private Long targetUserId(OaTaskRecord task) {
        if (task.assigneeMode() == AssigneeMode.USER) return task.assigneeUserId();
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

    private ProcessInstanceRecord mustInstance(long instanceId) {
        return store.findInstance(instanceId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Instance not found"));
    }
}
