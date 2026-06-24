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

    public List<RoleRecord> list(long actorUserId) {
        permissionService.require(actorUserId, "iam:role:read");
        return List.copyOf(store.roles());
    }

    public RoleRecord create(long actorUserId, RoleRequest request) {
        permissionService.require(actorUserId, "iam:role:write");
        RoleRecord role = store.createRole(request.code(), request.name(), request.description(), request.permissionCodes());
        auditService.record(actorUserId, "iam.role.create", "iam_role", role.code());
        return role;
    }

    public RoleRecord update(long actorUserId, String code, RoleRequest request) {
        permissionService.require(actorUserId, "iam:role:write");
        RoleRecord role = store.findRole(code).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Role not found"));
        role.update(request.name(), request.description(), request.permissionCodes());
        auditService.record(actorUserId, "iam.role.update", "iam_role", role.code());
        return role;
    }

    public record RoleRequest(String code, String name, String description, Set<String> permissionCodes) {
    }
}
