package com.health.platform.iam;

import java.util.LinkedHashSet;
import java.util.Set;

public class RoleRecord {
    private final String code;
    private String name;
    private String description;
    private final Set<String> permissionCodes = new LinkedHashSet<>();

    public RoleRecord(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String code() { return code; }
    public String name() { return name; }
    public String description() { return description; }
    public Set<String> permissionCodes() { return permissionCodes; }

    public void update(String name, String description, Set<String> permissionCodes) {
        if (name != null && !name.isBlank()) this.name = name;
        this.description = description;
        if (permissionCodes != null) {
            this.permissionCodes.clear();
            this.permissionCodes.addAll(permissionCodes);
        }
    }
    public String getCode() { return code(); }
    public String getName() { return name(); }
    public String getDescription() { return description(); }
    public Set<String> getPermissionCodes() { return permissionCodes(); }

}
