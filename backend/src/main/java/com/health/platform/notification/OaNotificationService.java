package com.health.platform.notification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.health.platform.iam.UserRecord;
import com.health.platform.wecom.WeComMessageClient;
import com.health.platform.wecom.WeComMessageResult;
import org.springframework.stereotype.Service;

@Service
public class OaNotificationService {
    private final List<MessageLog> messageLogs = new CopyOnWriteArrayList<>();
    private final OaNotificationRecipientResolver recipientResolver;
    private final OaNotificationMessageBuilder messageBuilder;
    private final WeComMessageClient messageClient;

    public OaNotificationService(OaNotificationRecipientResolver recipientResolver, OaNotificationMessageBuilder messageBuilder, WeComMessageClient messageClient) {
        this.recipientResolver = recipientResolver;
        this.messageBuilder = messageBuilder;
        this.messageClient = messageClient;
    }

    public void taskArrived(long taskId, Long targetUserId) {
        notify(taskId, targetUserId, "OA_TASK_ARRIVED");
    }

    public void manualUrge(long taskId, Long targetUserId) {
        notify(taskId, targetUserId, "OA_TASK_MANUAL_URGE");
    }

    public void autoRemind(long taskId, Long targetUserId) {
        notify(taskId, targetUserId, "OA_TASK_AUTO_REMIND");
    }

    public List<MessageLog> logs() {
        return new ArrayList<>(messageLogs);
    }

    private void notify(long taskId, Long targetUserId, String messageType) {
        List<UserRecord> recipients = recipientResolver.recipients(taskId, targetUserId);
        if (recipients.isEmpty()) {
            log(taskId, null, null, messageType, "skipped", "no recipient");
            return;
        }
        List<String> wecomIds = recipients.stream().map(UserRecord::wecomUserId).filter(id -> id != null && !id.isBlank()).toList();
        WeComMessageResult result = messageClient.sendTextCard(wecomIds, messageBuilder.title(messageType), messageBuilder.description(taskId, messageType), messageClient.mobileTaskUrl(taskId), "查看审批");
        for (UserRecord user : recipients) {
            String status = user.wecomUserId() == null || user.wecomUserId().isBlank() ? "failed" : result.status();
            String error = user.wecomUserId() == null || user.wecomUserId().isBlank() ? "user not bound wecomUserId" : result.errorMessage();
            log(taskId, user.id(), user.wecomUserId(), messageType, status, error);
        }
    }

    private void log(long taskId, Long targetUserId, String wecomUserId, String messageType, String status, String errorMessage) {
        messageLogs.add(new MessageLog(taskId, targetUserId, wecomUserId, messageType, status, errorMessage, OffsetDateTime.now()));
    }

    public record MessageLog(long taskId, Long targetUserId, String wecomUserId, String messageType, String status, String errorMessage, OffsetDateTime sentAt) {
    }
}
