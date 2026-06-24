package com.health.platform.notification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class OaNotificationService {
    private final List<MessageLog> messageLogs = new CopyOnWriteArrayList<>();

    public void taskArrived(long taskId, Long targetUserId) {
        log(taskId, targetUserId, "OA_TASK_ARRIVED", "sent", null);
    }

    public void manualUrge(long taskId, Long targetUserId) {
        log(taskId, targetUserId, "OA_TASK_MANUAL_URGE", "sent", null);
    }

    public void autoRemind(long taskId, Long targetUserId) {
        log(taskId, targetUserId, "OA_TASK_AUTO_REMIND", "sent", null);
    }

    public List<MessageLog> logs() {
        return new ArrayList<>(messageLogs);
    }

    private void log(long taskId, Long targetUserId, String messageType, String status, String errorMessage) {
        messageLogs.add(new MessageLog(taskId, targetUserId, messageType, status, errorMessage, OffsetDateTime.now()));
    }

    public record MessageLog(long taskId, Long targetUserId, String messageType, String status, String errorMessage, OffsetDateTime sentAt) {
    }
}
