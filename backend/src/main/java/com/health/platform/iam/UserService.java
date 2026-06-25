package com.health.platform.iam;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final String DEFAULT_PASSWORD = "qwer1234";

    private final IamStore store;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public UserService(IamStore store, PermissionService permissionService, AuditService auditService) {
        this.store = store;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    private void requireSystemAdmin(long actorUserId) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
    }

    public List<UserRecord> list(long actorUserId) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:read");
        return List.copyOf(store.users());
    }

    public UserRecord get(long actorUserId, long userId) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:read");
        return mustUser(userId);
    }

    public UserRecord create(long actorUserId, UserRequest request) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:create");
        UserRecord user = store.createUser(
            request.username(), request.displayName(), request.reportToUserId(),
            request.wecomUserId(), request.departmentName(), request.roleCodes());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        auditService.record(actorUserId, "iam.user.create", "iam_user", String.valueOf(user.id()));
        return user;
    }

    public UserRecord update(long actorUserId, long userId, UserRequest request) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:update");
        UserRecord user = mustUser(userId);
        user.update(request.username(), request.displayName(), request.reportToUserId(), request.wecomUserId(), request.departmentName());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.roleCodes() != null) {
            user.roleCodes().clear();
            user.roleCodes().addAll(request.roleCodes());
        }
        auditService.record(actorUserId, "iam.user.update", "iam_user", String.valueOf(user.id()));
        return user;
    }

    public UserRecord updateStatus(long actorUserId, long userId, boolean enabled) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:disable");
        UserRecord user = mustUser(userId);
        user.setEnabled(enabled);
        auditService.record(actorUserId, enabled ? "iam.user.enable" : "iam.user.disable", "iam_user", String.valueOf(user.id()));
        return user;
    }

    public UserRecord assignRoles(long actorUserId, long userId, Set<String> roleCodes) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:update");
        UserRecord user = mustUser(userId);
        user.roleCodes().clear();
        user.roleCodes().addAll(roleCodes == null ? Set.of() : roleCodes);
        auditService.record(actorUserId, "iam.user.roles.update", "iam_user", String.valueOf(user.id()));
        return user;
    }

    public UserRecord updatePermissionOverrides(long actorUserId, long userId, Map<String, PermissionEffect> overrides, String reason) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user-permission:write");
        UserRecord user = mustUser(userId);
        user.permissionOverrides().clear();
        if (overrides != null) {
            user.permissionOverrides().putAll(overrides);
        }
        auditService.record(actorUserId, "iam.user.permission_overrides.update", "iam_user", String.valueOf(user.id()) + ":" + (reason == null ? "" : reason));
        return user;
    }

    public UserRecord resetPassword(long actorUserId, long userId) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:update");
        UserRecord user = mustUser(userId);
        user.setPasswordHash(PasswordHasher.sha256(DEFAULT_PASSWORD));
        user.setMustChangePassword(true);
        auditService.record(actorUserId, "iam.user.password.reset", "iam_user", String.valueOf(user.id()));
        return user;
    }

    public void delete(long actorUserId, long userId) {
        requireSystemAdmin(actorUserId);
        permissionService.require(actorUserId, "iam:user:delete");
        mustUser(userId);
        store.softDeleteUser(userId);
        auditService.record(actorUserId, "iam.user.delete", "iam_user", String.valueOf(userId));
    }

    public UserRecord mustUser(long userId) {
        return store.findUser(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
    }

    public record UserRequest(
        String username,
        String displayName,
        Long reportToUserId,
        String wecomUserId,
        String departmentName,
        Set<String> roleCodes,
        Boolean enabled
    ) {
        public UserRequest(String username, String displayName, Long reportToUserId, String wecomUserId, String departmentName) {
            this(username, displayName, reportToUserId, wecomUserId, departmentName, null, null);
        }
    }
}
