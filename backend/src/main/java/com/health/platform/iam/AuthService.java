package com.health.platform.iam;

import java.util.Set;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final IamStore store;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public AuthService(IamStore store, PermissionService permissionService, AuditService auditService) {
        this.store = store;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Username and password are required");
        }
        UserRecord user = store.findUserByUsername(request.username().trim())
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Invalid username or password"));
        if (!user.enabled() || !PasswordHasher.sha256(request.password()).equals(user.passwordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Invalid username or password");
        }
        Set<String> permissions = permissionService.effectivePermissions(user.id());
        auditService.record(user.id(), "iam.login", "iam_user", String.valueOf(user.id()));
        return new LoginResponse(user.id(), user.username(), user.displayName(), user.roleCodes(), permissions);
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(long userId, String username, String displayName, Set<String> roleCodes, Set<String> permissions) {
    }
}
