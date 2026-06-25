package com.health.platform.oa;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcessDefinitionRecord {
    private final long id;
    private final String code;
    private String name;
    private final int version;
    private String status = "enabled";
    private boolean builtin;
    private String description = "";
    private String businessType = "";
    private Map<String, Object> formSchema = new LinkedHashMap<>();
    private long createdBy;
    private long updatedBy;
    private OffsetDateTime publishedAt;
    private OffsetDateTime deletedAt;
    private boolean enabled = true;
    private final List<ProcessNodeRecord> nodes = new ArrayList<>();

    public ProcessDefinitionRecord(long id, String code, String name, int version) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.version = version;
    }

    public long id() { return id; }
    public String code() { return code; }
    public String name() { return name; }
    public int version() { return version; }
    public String status() { return status; }
    public boolean builtin() { return builtin; }
    public String description() { return description; }
    public String businessType() { return businessType; }
    public Map<String, Object> formSchema() { return formSchema; }
    public long createdBy() { return createdBy; }
    public long updatedBy() { return updatedBy; }
    public OffsetDateTime publishedAt() { return publishedAt; }
    public OffsetDateTime deletedAt() { return deletedAt; }
    public boolean enabled() { return enabled; }
    public List<ProcessNodeRecord> nodes() { return nodes; }

    public void updateBasic(String name, String description, String businessType, Map<String, Object> formSchema, Boolean enabled) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (businessType != null) this.businessType = businessType;
        if (formSchema != null) this.formSchema = new LinkedHashMap<>(formSchema);
        if (enabled != null) {
            this.enabled = enabled;
            this.status = enabled ? "enabled" : "disabled";
        }
    }

    public void markDraft() { this.status = "draft"; this.enabled = false; }
    public void markPublished(long actorUserId) {
        this.status = "enabled";
        this.enabled = true;
        this.publishedAt = OffsetDateTime.now();
        this.updatedBy = actorUserId;
    }
    public void markDisabled() { this.status = "disabled"; this.enabled = false; }
    public void markArchived() { this.status = "archived"; this.enabled = false; this.deletedAt = OffsetDateTime.now(); }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }
    public void setCreatedBy(long createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(long updatedBy) { this.updatedBy = updatedBy; }

    public long getId() { return id(); }
    public String getCode() { return code(); }
    public String getName() { return name(); }
    public int getVersion() { return version(); }
    public String getStatus() { return status(); }
    public boolean isBuiltin() { return builtin(); }
    public String getDescription() { return description(); }
    public String getBusinessType() { return businessType(); }
    public Map<String, Object> getFormSchema() { return formSchema(); }
    public long getCreatedBy() { return createdBy(); }
    public long getUpdatedBy() { return updatedBy(); }
    public OffsetDateTime getPublishedAt() { return publishedAt(); }
    public OffsetDateTime getDeletedAt() { return deletedAt(); }
    public boolean isEnabled() { return enabled(); }
    public List<ProcessNodeRecord> getNodes() { return nodes(); }
}
