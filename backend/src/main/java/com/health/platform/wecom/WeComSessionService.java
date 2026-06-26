package com.health.platform.wecom;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class WeComSessionService {
    private final Map<String, WeComSessionRecord> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final IamStore iamStore;

    public WeComSessionService(IamStore iamStore) {
        this.iamStore = iamStore;
    }

    public WeComSessionRecord createSession(UserRecord user, String wecomUserId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        WeComSessionRecord session = new WeComSessionRecord(token, user.id(), wecomUserId, OffsetDateTime.now().plusHours(12), OffsetDateTime.now());
        sessions.put(token, session);
        return session;
    }

    public long requireUserId(String authorization) {
        return resolveUserId(authorization).orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Invalid mobile token"));
    }

    public Optional<Long> resolveUserId(String authorization) {
        String token = bearerToken(authorization);
        if (token == null) return Optional.empty();
        WeComSessionRecord session = sessions.get(token);
        if (session == null || session.expired()) {
            if (session != null) sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.userId());
    }

    public Optional<UserRecord> currentUser(String authorization) {
        return resolveUserId(authorization).flatMap(iamStore::findUser);
    }

    public void logout(String authorization) {
        String token = bearerToken(authorization);
        if (token != null) sessions.remove(token);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isBlank() ? null : token;
    }
}
