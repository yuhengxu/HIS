package com.health.platform.oa;

public class ProcessNodeRecord {
    private final long id;
    private final long processDefinitionId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private int sortOrder;
    private AssigneeMode assigneeMode;
    private Long assigneeUserId;
    private String assigneeRoleCode;
    private String approvePolicy = "ANY_ONE";
    private String rejectPolicy = "BACK_TO_START";
    private boolean requireSelfOnly = true;
    private int supervisorLevel = 1;
    private Integer timeoutHours = 24;

    public ProcessNodeRecord(long id, long processDefinitionId, String nodeCode, String nodeName, String nodeType,
                             int sortOrder, AssigneeMode assigneeMode, Long assigneeUserId, String assigneeRoleCode,
                             boolean requireSelfOnly, int supervisorLevel, Integer timeoutHours) {
        this.id = id;
        this.processDefinitionId = processDefinitionId;
        this.nodeCode = nodeCode;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.sortOrder = sortOrder;
        this.assigneeMode = assigneeMode;
        this.assigneeUserId = assigneeUserId;
        this.assigneeRoleCode = assigneeRoleCode;
        this.requireSelfOnly = requireSelfOnly;
        this.supervisorLevel = supervisorLevel;
        this.timeoutHours = timeoutHours;
    }

    public long id() { return id; }
    public long processDefinitionId() { return processDefinitionId; }
    public String nodeCode() { return nodeCode; }
    public String nodeName() { return nodeName; }
    public String nodeType() { return nodeType; }
    public int sortOrder() { return sortOrder; }
    public AssigneeMode assigneeMode() { return assigneeMode; }
    public Long assigneeUserId() { return assigneeUserId; }
    public String assigneeRoleCode() { return assigneeRoleCode; }
    public String approvePolicy() { return approvePolicy; }
    public String rejectPolicy() { return rejectPolicy; }
    public boolean requireSelfOnly() { return requireSelfOnly; }
    public int supervisorLevel() { return supervisorLevel; }
    public Integer timeoutHours() { return timeoutHours; }

    public void updateFrom(NodeRequest request) {
        if (request.nodeCode() != null) this.nodeCode = request.nodeCode();
        if (request.nodeName() != null) this.nodeName = request.nodeName();
        if (request.nodeType() != null) this.nodeType = request.nodeType();
        if (request.sortOrder() != null) this.sortOrder = request.sortOrder();
        if (request.assigneeMode() != null) this.assigneeMode = request.assigneeMode();
        this.assigneeUserId = request.assigneeUserId();
        this.assigneeRoleCode = request.assigneeRoleCode();
        if (request.approvePolicy() != null) this.approvePolicy = request.approvePolicy();
        if (request.rejectPolicy() != null) this.rejectPolicy = request.rejectPolicy();
        if (request.requireSelfOnly() != null) this.requireSelfOnly = request.requireSelfOnly();
        if (request.supervisorLevel() != null) this.supervisorLevel = request.supervisorLevel();
        if (request.timeoutHours() != null) this.timeoutHours = request.timeoutHours();
    }

    public record NodeRequest(String nodeCode, String nodeName, String nodeType, Integer sortOrder, AssigneeMode assigneeMode,
                              Long assigneeUserId, String assigneeRoleCode, String approvePolicy, String rejectPolicy,
                              Boolean requireSelfOnly, Integer supervisorLevel, Integer timeoutHours) {
    }

    public long getId() { return id(); }
    public long getProcessDefinitionId() { return processDefinitionId(); }
    public String getNodeCode() { return nodeCode(); }
    public String getNodeName() { return nodeName(); }
    public String getNodeType() { return nodeType(); }
    public int getSortOrder() { return sortOrder(); }
    public AssigneeMode getAssigneeMode() { return assigneeMode(); }
    public Long getAssigneeUserId() { return assigneeUserId(); }
    public String getAssigneeRoleCode() { return assigneeRoleCode(); }
    public String getApprovePolicy() { return approvePolicy(); }
    public String getRejectPolicy() { return rejectPolicy(); }
    public boolean isRequireSelfOnly() { return requireSelfOnly(); }
    public int getSupervisorLevel() { return supervisorLevel(); }
    public Integer getTimeoutHours() { return timeoutHours(); }
}
