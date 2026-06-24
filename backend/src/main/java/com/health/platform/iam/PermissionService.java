package com.health.platform.iam;

import java.util.LinkedHashSet;
import java.util.Set;

import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final IamStore store;

    public PermissionService(IamStore store) {
        this.store = store;
    }

    public Set<String> effectivePermissions(long userId) {
        UserRecord user = store.findUser(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        if (!user.enabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "User is disabled");
        }
        Set<String> permissions = new LinkedHashSet<>();
        for (String roleCode : user.roleCodes()) {
            store.findRole(roleCode).ifPresent(role -> permissions.addAll(role.permissionCodes()));
        }
        user.permissionOverrides().forEach((permission, effect) -> {
            if (effect == PermissionEffect.GRANT) {
                permissions.add(permission);
            } else if (effect == PermissionEffect.DENY) {
                permissions.remove(permission);
            }
        });
        return permissions;
    }

    public boolean hasPermission(long userId, String permissionCode) {
        return effectivePermissions(userId).contains(permissionCode);
    }

    public void require(long userId, String permissionCode) {
        if (!hasPermission(userId, permissionCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing permission: " + permissionCode);
        }
    }
}
