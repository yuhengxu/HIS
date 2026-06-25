package com.health.platform.oa;

import java.time.OffsetDateTime;

public class OaTaskRecord {
    private final long id;
    private final long processInstanceId;
    private final long nodeId;
    private final AssigneeMode assigneeMode;
    private final Long assigneeUserId;
    private final String assigneeRoleCode;
    private final Long resolvedSupervisorUserId;
    private TaskStatus status = TaskStatus.PENDING;
    private Long claimedByUserId;
    private final OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime handledAt;
    private int remindCount;

    public OaTaskRecord(long id, long processInstanceId, long nodeId, AssigneeMode assigneeMode, Long assigneeUserId, String assigneeRoleCode, Long resolvedSupervisorUserId) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.nodeId = nodeId;
        this.assigneeMode = assigneeMode;
        this.assigneeUserId = assigneeUserId;
        this.assigneeRoleCode = assigneeRoleCode;
        this.resolvedSupervisorUserId = resolvedSupervisorUserId;
    }

    public long id() { return id; }
    public long processInstanceId() { return processInstanceId; }
    public long nodeId() { return nodeId; }
    public AssigneeMode assigneeMode() { return assigneeMode; }
    public Long assigneeUserId() { return assigneeUserId; }
    public String assigneeRoleCode() { return assigneeRoleCode; }
    public Long resolvedSupervisorUserId() { return resolvedSupervisorUserId; }
    public TaskStatus status() { return status; }
    public Long claimedByUserId() { return claimedByUserId; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime handledAt() { return handledAt; }
    public int remindCount() { return remindCount; }

    public void approve(long actorUserId) { this.status = TaskStatus.APPROVED; this.claimedByUserId = actorUserId; this.handledAt = OffsetDateTime.now(); }
    public void reject(long actorUserId) { this.status = TaskStatus.REJECTED; this.claimedByUserId = actorUserId; this.handledAt = OffsetDateTime.now(); }
    public void cancel(long actorUserId) { this.status = TaskStatus.CANCELLED; this.claimedByUserId = actorUserId; this.handledAt = OffsetDateTime.now(); }
    public void incrementRemindCount() { this.remindCount++; }
    public long getId() { return id(); }
    public long getProcessInstanceId() { return processInstanceId(); }
    public long getNodeId() { return nodeId(); }
    public AssigneeMode getAssigneeMode() { return assigneeMode(); }
    public Long getAssigneeUserId() { return assigneeUserId(); }
    public String getAssigneeRoleCode() { return assigneeRoleCode(); }
    public Long getResolvedSupervisorUserId() { return resolvedSupervisorUserId(); }
    public TaskStatus getStatus() { return status(); }
    public Long getClaimedByUserId() { return claimedByUserId(); }
    public OffsetDateTime getCreatedAt() { return createdAt(); }
    public OffsetDateTime getHandledAt() { return handledAt(); }
    public int getRemindCount() { return remindCount(); }

}
