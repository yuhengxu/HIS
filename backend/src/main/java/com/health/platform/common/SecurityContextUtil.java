package com.health.platform.common;

public final class SecurityContextUtil {
    private SecurityContextUtil() {
    }

    public static long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Missing X-User-Id header");
        }
        return userId;
    }
}
