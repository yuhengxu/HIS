package com.health.platform.audit;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

    public void record(long actorUserId, String action, String resourceType, String resourceId) {
        events.add(new AuditEvent(actorUserId, action, resourceType, resourceId, OffsetDateTime.now()));
    }

    public List<AuditEvent> list() {
        return new ArrayList<>(events);
    }

    public record AuditEvent(long actorUserId, String action, String resourceType, String resourceId, OffsetDateTime createdAt) {
    }
}
