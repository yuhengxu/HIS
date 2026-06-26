package com.health.platform.notification;

import java.util.ArrayList;
import java.util.List;

import com.health.platform.iam.IamStore;
import com.health.platform.iam.UserRecord;
import com.health.platform.oa.AssigneeMode;
import com.health.platform.oa.OaStore;
import com.health.platform.oa.OaTaskRecord;
import org.springframework.stereotype.Component;

@Component
public class OaNotificationRecipientResolver {
    private final OaStore oaStore;
    private final IamStore iamStore;

    public OaNotificationRecipientResolver(OaStore oaStore, IamStore iamStore) {
        this.oaStore = oaStore;
        this.iamStore = iamStore;
    }

    public List<UserRecord> recipients(long taskId, Long fallbackUserId) {
        OaTaskRecord task = oaStore.findTask(taskId).orElse(null);
        if (task == null) return fallback(fallbackUserId);
        if (task.assigneeMode() == AssigneeMode.ROLE && task.assigneeRoleCode() != null) {
            return iamStore.usersWithRole(task.assigneeRoleCode());
        }
        Long userId = null;
        if (task.assigneeMode() == AssigneeMode.USER || task.assigneeMode() == AssigneeMode.INITIATOR_SELECTED) {
            userId = task.assigneeUserId();
        } else if (task.assigneeMode() == AssigneeMode.SUPERVISOR) {
            userId = task.resolvedSupervisorUserId();
        }
        return fallback(userId == null ? fallbackUserId : userId);
    }

    private List<UserRecord> fallback(Long userId) {
        if (userId == null) return List.of();
        return iamStore.findUser(userId).map(List::of).orElseGet(ArrayList::new);
    }
}
