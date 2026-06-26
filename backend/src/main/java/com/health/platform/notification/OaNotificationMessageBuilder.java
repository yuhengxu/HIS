package com.health.platform.notification;

import com.health.platform.oa.OaStore;
import com.health.platform.oa.OaTaskRecord;
import com.health.platform.oa.ProcessInstanceRecord;
import com.health.platform.oa.ProcessNodeRecord;
import org.springframework.stereotype.Component;

@Component
public class OaNotificationMessageBuilder {
    private final OaStore store;

    public OaNotificationMessageBuilder(OaStore store) {
        this.store = store;
    }

    public String title(String messageType) {
        if ("OA_TASK_MANUAL_URGE".equals(messageType)) return "OA 催办提醒";
        if ("OA_TASK_AUTO_REMIND".equals(messageType)) return "OA 自动提醒";
        return "你有一个新的 OA 待办";
    }

    public String description(long taskId, String messageType) {
        OaTaskRecord task = store.findTask(taskId).orElse(null);
        if (task == null) return "请进入和悦医养 OA 查看待办。";
        ProcessInstanceRecord instance = store.findInstance(task.processInstanceId()).orElse(null);
        String nodeName = store.findNode(task.nodeId()).map(ProcessNodeRecord::nodeName).orElse("审批节点");
        String title = instance == null ? "OA 流程" : instance.title();
        String action = "OA_TASK_MANUAL_URGE".equals(messageType) ? "发起人催办了当前流程" : "流程已流转到你处理";
        return "<div class=\"normal\">" + escape(title) + "</div><div class=\"gray\">" + escape(nodeName) + "</div><div class=\"highlight\">" + escape(action) + "</div>";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
