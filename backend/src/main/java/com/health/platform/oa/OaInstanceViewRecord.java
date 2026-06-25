package com.health.platform.oa;

public record OaInstanceViewRecord(
    long id,
    String processCode,
    String businessType,
    String title,
    InstanceStatus status,
    long initiatorUserId,
    Long currentTaskId,
    String currentNodeName,
    String currentHandler,
    String stage
) {
}
