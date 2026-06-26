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

    public interface MobileTokenResolver {
        long requireUserId(String authorization);
    }

    public static long requireUserId(Long userId, String authorization, MobileTokenResolver resolver) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return resolver.requireUserId(authorization);
        }
        return requireUserId(userId);
    }
}
