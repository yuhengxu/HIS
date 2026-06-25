package com.health.platform.oa;

import java.time.OffsetDateTime;
import java.util.Map;

public record OaTaskDetailRecord(
    long taskId,
    long processInstanceId,
    String processCode,
    String businessType,
    String title,
    InstanceStatus instanceStatus,
    long initiatorUserId,
    String initiatorName,
    long nodeId,
    String nodeName,
    AssigneeMode assigneeMode,
    TaskStatus taskStatus,
    OffsetDateTime createdAt,
    Map<String, Object> formData,
    Map<String, Object> displayData
) {
}
