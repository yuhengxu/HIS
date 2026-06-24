package com.health.platform.iam;

import java.util.LinkedHashSet;
import java.util.Set;

public class PermissionRecord {
    private final String code;
    private String name;
    private String domain;
    private String resourceType;
    private String description;
    private boolean systemBuiltIn;
    private boolean enabled = true;
    private final Set<String> defaultRoleCodes = new LinkedHashSet<>();

    public PermissionRecord(String code, String name, String domain, String resourceType, String description, boolean systemBuiltIn, String... defaultRoles) {
        this.code = code;
        this.name = name;
        this.domain = domain;
        this.resourceType = resourceType;
        this.description = description;
        this.systemBuiltIn = systemBuiltIn;
        if (defaultRoles != null) {
            for (String role : defaultRoles) {
                this.defaultRoleCodes.add(role);
            }
        }
    }

    public String code() { return code; }
    public String name() { return name; }
    public String domain() { return domain; }
    public String resourceType() { return resourceType; }
    public String description() { return description; }
    public boolean systemBuiltIn() { return systemBuiltIn; }
    public boolean enabled() { return enabled; }
    public Set<String> defaultRoleCodes() { return defaultRoleCodes; }

    public String getCode() { return code(); }
    public String getName() { return name(); }
    public String getDomain() { return domain(); }
    public String getResourceType() { return resourceType(); }
    public String getDescription() { return description(); }
    public boolean isSystemBuiltIn() { return systemBuiltIn(); }
    public boolean isEnabled() { return enabled(); }
    public Set<String> getDefaultRoleCodes() { return defaultRoleCodes(); }
}
