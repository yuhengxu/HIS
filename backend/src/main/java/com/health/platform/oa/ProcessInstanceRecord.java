package com.health.platform.oa;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessInstanceRecord {
    private final long id;
    private final long processDefinitionId;
    private final String processCode;
    private final String businessType;
    private final String title;
    private final long initiatorUserId;
    private final Map<String, Object> formData = new LinkedHashMap<>();
    private InstanceStatus status = InstanceStatus.RUNNING;
    private Long currentNodeId;
    private OffsetDateTime finishedAt;

    public ProcessInstanceRecord(long id, long processDefinitionId, String processCode, String businessType, String title, long initiatorUserId, Map<String, Object> formData) {
        this.id = id;
        this.processDefinitionId = processDefinitionId;
        this.processCode = processCode;
        this.businessType = businessType;
        this.title = title;
        this.initiatorUserId = initiatorUserId;
        if (formData != null) {
            this.formData.putAll(formData);
        }
    }

    public long id() { return id; }
    public long processDefinitionId() { return processDefinitionId; }
    public String processCode() { return processCode; }
    public String businessType() { return businessType; }
    public String title() { return title; }
    public long initiatorUserId() { return initiatorUserId; }
    public Map<String, Object> formData() { return formData; }
    public InstanceStatus status() { return status; }
    public Long currentNodeId() { return currentNodeId; }
    public OffsetDateTime finishedAt() { return finishedAt; }

    public void moveTo(Long nodeId) { this.currentNodeId = nodeId; }
    public void markApproved() { this.status = InstanceStatus.APPROVED; this.finishedAt = OffsetDateTime.now(); }
    public void markRejected() { this.status = InstanceStatus.REJECTED; this.finishedAt = OffsetDateTime.now(); }
    public void markPendingConfig() { this.status = InstanceStatus.PENDING_CONFIG; }
    public long getId() { return id(); }
    public long getProcessDefinitionId() { return processDefinitionId(); }
    public String getProcessCode() { return processCode(); }
    public String getBusinessType() { return businessType(); }
    public String getTitle() { return title(); }
    public long getInitiatorUserId() { return initiatorUserId(); }
    public Map<String, Object> getFormData() { return formData(); }
    public InstanceStatus getStatus() { return status(); }
    public Long getCurrentNodeId() { return currentNodeId(); }
    public OffsetDateTime getFinishedAt() { return finishedAt(); }

}
