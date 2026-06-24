package com.health.platform.oa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import com.health.platform.notification.OaNotificationService;
import org.springframework.stereotype.Service;

@Service
public class ProcessRuntimeService {
    private final OaStore store;
    private final IamStore iamStore;
    private final PermissionService permissionService;
    private final SupervisorResolver supervisorResolver;
    private final OaNotificationService notificationService;
    private final AuditService auditService;
    private final List<OaApprovalListener> approvalListeners;

    public ProcessRuntimeService(OaStore store, IamStore iamStore, PermissionService permissionService, SupervisorResolver supervisorResolver, OaNotificationService notificationService, AuditService auditService, List<OaApprovalListener> approvalListeners) {
        this.store = store;
        this.iamStore = iamStore;
        this.permissionService = permissionService;
        this.supervisorResolver = supervisorResolver;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.approvalListeners = approvalListeners == null ? List.of() : approvalListeners;
    }

    public List<ProcessDefinitionRecord> definitions(long actorUserId) {
        permissionService.require(actorUserId, "oa:process:read");
        return List.copyOf(store.definitions());
    }

    public ProcessInstanceRecord start(long actorUserId, String processCode, String businessType, String title, Map<String, Object> formData) {
        permissionService.require(actorUserId, "oa:instance:create");
        ProcessInstanceRecord instance = store.addInstance(processCode, businessType, title, actorUserId, formData);
        ProcessNodeRecord firstNode = store.sortedNodes(instance.processDefinitionId()).stream().findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, "Process has no node"));
        createTask(instance, firstNode);
        auditService.record(actorUserId, "oa.instance.create", "oa_process_instance", String.valueOf(instance.id()));
        return instance;
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
