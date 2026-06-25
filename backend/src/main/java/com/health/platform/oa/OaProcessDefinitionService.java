package com.health.platform.oa;

import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class OaProcessDefinitionService {
    private final OaStore store;
    private final IamStore iamStore;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public OaProcessDefinitionService(OaStore store, IamStore iamStore, PermissionService permissionService, AuditService auditService) {
        this.store = store;
        this.iamStore = iamStore;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    public List<ProcessDefinitionRecord> list(long actorUserId) {
        permissionService.require(actorUserId, "oa:process:read");
        return List.copyOf(store.definitions());
    }

    public ProcessDefinitionRecord get(long actorUserId, long id) {
        permissionService.require(actorUserId, "oa:process:read");
        return store.findDefinition(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Process not found"));
    }

    public ProcessDefinitionRecord create(long actorUserId, DefinitionRequest request) {
        requireProcessWriter(actorUserId, "oa:process-definition:create");
        ProcessDefinitionRecord definition = store.createDefinition(request.code(), request.name(), request.businessType(), request.description(), actorUserId);
        if (request.nodes() != null && !request.nodes().isEmpty()) {
            store.saveNodes(definition.id(), request.nodes());
        }
        auditService.record(actorUserId, "oa.process_definition.create", "oa_process_definition", String.valueOf(definition.id()));
        return definition;
    }

    public ProcessDefinitionRecord update(long actorUserId, long id, DefinitionRequest request) {
        requireProcessWriter(actorUserId, "oa:process-definition:update");
        ProcessDefinitionRecord definition = get(actorUserId, id);
        if (store.countInstancesByDefinition(id) > 0 && request.nodes() != null) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Process with instances must create new version for node changes");
        }
        definition.updateBasic(request.name(), request.description(), request.businessType(), request.formSchema(), request.enabled());
        definition.setUpdatedBy(actorUserId);
        auditService.record(actorUserId, "oa.process_definition.update", "oa_process_definition", String.valueOf(id));
        return definition;
    }

    public ProcessDefinitionRecord saveNodes(long actorUserId, long id, List<ProcessNodeRecord.NodeRequest> nodes) {
        permissionService.requireAny(actorUserId, "oa:process-node:write", "oa:process:write");
        requireProcessWriter(actorUserId, "oa:process-definition:update");
        ProcessDefinitionRecord definition = get(actorUserId, id);
        if (store.countInstancesByDefinition(id) > 0) {
            ProcessDefinitionRecord next = store.newVersionFrom(id, actorUserId);
            validateNodes(nodes);
            store.saveNodes(next.id(), nodes);
            auditService.record(actorUserId, "oa.process_definition.version.create", "oa_process_definition", String.valueOf(next.id()));
            return next;
        }
        validateNodes(nodes);
        store.saveNodes(id, nodes);
        auditService.record(actorUserId, "oa.process_node.save", "oa_process_definition", String.valueOf(id));
        return store.findDefinition(id).orElseThrow();
    }

    public void validateNodes(long actorUserId, long id, List<ProcessNodeRecord.NodeRequest> nodes) {
        permissionService.requireAny(actorUserId, "oa:process-node:write", "oa:process:write");
        validateNodes(nodes);
    }

    public ProcessDefinitionRecord publish(long actorUserId, long id) {
        requireProcessWriter(actorUserId, "oa:process-definition:publish");
        ProcessDefinitionRecord definition = get(actorUserId, id);
        validateNodes(definition.nodes().stream()
            .map(n -> new ProcessNodeRecord.NodeRequest(n.nodeCode(), n.nodeName(), n.nodeType(), n.sortOrder(), n.assigneeMode(),
                n.assigneeUserId(), n.assigneeRoleCode(), n.approvePolicy(), n.rejectPolicy(), n.requireSelfOnly(), n.supervisorLevel(), n.timeoutHours()))
            .toList());
        definition.markPublished(actorUserId);
        auditService.record(actorUserId, "oa.process_definition.publish", "oa_process_definition", String.valueOf(id));
        return definition;
    }

    public ProcessDefinitionRecord copy(long actorUserId, long id, CopyRequest request) {
        requireProcessWriter(actorUserId, "oa:process-definition:create");
        ProcessDefinitionRecord copy = store.copyDefinition(id, request.newCode(), request.newName(), actorUserId);
        auditService.record(actorUserId, "oa.process_definition.copy", "oa_process_definition", String.valueOf(copy.id()));
        return copy;
    }

    public void delete(long actorUserId, long id) {
        requireProcessWriter(actorUserId, "oa:process-definition:delete");
        ProcessDefinitionRecord definition = get(actorUserId, id);
        if (definition.builtin()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Built-in process cannot be deleted");
        }
        if (store.countInstancesByDefinition(id) > 0) {
            definition.markArchived();
        } else {
            definition.markArchived();
        }
        auditService.record(actorUserId, "oa.process_definition.delete", "oa_process_definition", String.valueOf(id));
    }

    private void requireProcessWriter(long actorUserId, String permission) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "OA_ADMIN");
        permissionService.requireAny(actorUserId, permission, "oa:process:write");
    }

    private void validateNodes(List<ProcessNodeRecord.NodeRequest> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Process must have at least one node");
        }
        for (ProcessNodeRecord.NodeRequest node : nodes) {
            if (node.assigneeMode() == AssigneeMode.USER && node.assigneeUserId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "USER node requires assigneeUserId");
            }
            if (node.assigneeMode() == AssigneeMode.ROLE && (node.assigneeRoleCode() == null || node.assigneeRoleCode().isBlank())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ROLE node requires assigneeRoleCode");
            }
            if (node.assigneeMode() == AssigneeMode.ROLE && iamStore.roles().stream()
                .noneMatch(role -> role.code().equals(node.assigneeRoleCode()))) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Role not found: " + node.assigneeRoleCode());
            }
        }
    }

    public record DefinitionRequest(String code, String name, String businessType, String description, Map<String, Object> formSchema, Boolean enabled, List<ProcessNodeRecord.NodeRequest> nodes) {
    }

    public record CopyRequest(String newCode, String newName) {
    }
}
