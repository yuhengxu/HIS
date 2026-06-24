package com.health.platform.iam;

import java.util.List;
import java.util.Set;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    private final IamStore store;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public RoleService(IamStore store, PermissionService permissionService, AuditService auditService) {
        this.store = store;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    private void requireSystemAdmin(long actorUserId) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
    }

    public List<RoleRecord> list(long actorUserId) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:role:read");
        return List.copyOf(store.roles());
    }

    public RoleRecord get(long actorUserId, String code) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:role:read");
        return store.findRole(code).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Role not found"));
    }

    public RoleRecord create(long actorUserId, RoleRequest request) {
        requireSystemAdmin(actorUserId);
        permissionService.requireAny(actorUserId, "iam:role:create", "iam:role:write");
        RoleRecord role = store.createRole(request.code(), request.name(), request.description(), request.permissionCodes(), request.sortOrder());
        auditService.record(actorUserId, "iam.role.create", "iam_role", role.code());
        return role;
    }

    public RoleRecord update(long actorUserId, String code, RoleRequest request) {
        requireSystemAdmin(actorUserId);
        permissionService.requireAny(actorUserId, "iam:role:update", "iam:role:write");
        RoleRecord role = store.findRole(code).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Role not found"));
        if (role.systemBuiltIn() && request.enabled() != null && !request.enabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "System built-in role cannot be disabled");
        }
        role.update(request.name(), request.description(), request.permissionCodes(), request.enabled(), request.sortOrder());
        auditService.record(actorUserId, "iam.role.update", "iam_role", role.code());
        return role;
    }

    public void delete(long actorUserId, String code) {
        requireSystemAdmin(actorUserId);
        permissionService.requireAny(actorUserId, "iam:role:delete", "iam:role:write");
        store.softDeleteRole(code);
        auditService.record(actorUserId, "iam.role.delete", "iam_role", code);
    }

    public record RoleRequest(String code, String name, String description, Set<String> permissionCodes, Boolean enabled, Integer sortOrder) {
    }
}
