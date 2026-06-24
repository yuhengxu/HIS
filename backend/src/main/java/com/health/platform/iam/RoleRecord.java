package com.health.platform.iam;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class RoleRecord {
    private final String code;
    private String name;
    private String description;
    private boolean enabled = true;
    private int sortOrder;
    private boolean systemBuiltIn;
    private OffsetDateTime deletedAt;
    private final Set<String> permissionCodes = new LinkedHashSet<>();

    public RoleRecord(String code, String name, String description, boolean systemBuiltIn, int sortOrder) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemBuiltIn = systemBuiltIn;
        this.sortOrder = sortOrder;
    }

    public String code() { return code; }
    public String name() { return name; }
    public String description() { return description; }
    public boolean enabled() { return enabled; }
    public int sortOrder() { return sortOrder; }
    public boolean systemBuiltIn() { return systemBuiltIn; }
    public OffsetDateTime deletedAt() { return deletedAt; }
    public Set<String> permissionCodes() { return permissionCodes; }

    public void update(String name, String description, Set<String> permissionCodes, Boolean enabled, Integer sortOrder) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (enabled != null) this.enabled = enabled;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (permissionCodes != null) {
            this.permissionCodes.clear();
            this.permissionCodes.addAll(permissionCodes);
        }
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.enabled = false;
    }

    public String getCode() { return code(); }
    public String getName() { return name(); }
    public String getDescription() { return description(); }
    public boolean isEnabled() { return enabled(); }
    public int getSortOrder() { return sortOrder(); }
    public boolean isSystemBuiltIn() { return systemBuiltIn(); }
    public OffsetDateTime getDeletedAt() { return deletedAt(); }
    public Set<String> getPermissionCodes() { return permissionCodes(); }
}
