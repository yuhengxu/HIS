package com.health.platform.wecom;

import java.time.OffsetDateTime;

public record WeComSessionRecord(String token, long userId, String wecomUserId, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
    public boolean expired() {
        return expiresAt.isBefore(OffsetDateTime.now());
    }
}
