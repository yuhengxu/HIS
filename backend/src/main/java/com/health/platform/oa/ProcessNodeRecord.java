package com.health.platform.oa;

public record ProcessNodeRecord(
    long id,
    long processDefinitionId,
    String nodeCode,
    String nodeName,
    String nodeType,
    int sortOrder,
    AssigneeMode assigneeMode,
    Long assigneeUserId,
    String assigneeRoleCode,
    boolean requireSelfOnly,
    int supervisorLevel,
    Integer timeoutHours
) {
}
