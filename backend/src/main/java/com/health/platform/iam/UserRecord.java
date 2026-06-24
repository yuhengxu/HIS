package com.health.platform.iam;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserRecord {
    private final long id;
    private String username;
    private String displayName;
    private boolean enabled = true;
    private Long reportToUserId;
    private String wecomUserId;
    private String departmentName;
    private String passwordHash;
    private final Set<String> roleCodes = new LinkedHashSet<>();
    private final Map<String, PermissionEffect> permissionOverrides = new LinkedHashMap<>();

    public UserRecord(long id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    public long id() { return id; }
    public String username() { return username; }
    public String displayName() { return displayName; }
    public boolean enabled() { return enabled; }
    public Long reportToUserId() { return reportToUserId; }
    public String wecomUserId() { return wecomUserId; }
    public String departmentName() { return departmentName; }
    @JsonIgnore
    public String passwordHash() { return passwordHash; }
    public Set<String> roleCodes() { return roleCodes; }
    public Map<String, PermissionEffect> permissionOverrides() { return permissionOverrides; }

    public void update(String username, String displayName, Long reportToUserId, String wecomUserId, String departmentName) {
        if (username != null && !username.isBlank()) this.username = username;
        if (displayName != null && !displayName.isBlank()) this.displayName = displayName;
        this.reportToUserId = reportToUserId;
        this.wecomUserId = wecomUserId;
        this.departmentName = departmentName;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public long getId() { return id(); }
    public String getUsername() { return username(); }
    public String getDisplayName() { return displayName(); }
    public boolean isEnabled() { return enabled(); }
    public Long getReportToUserId() { return reportToUserId(); }
    public String getWecomUserId() { return wecomUserId(); }
    public String getDepartmentName() { return departmentName(); }
    @JsonIgnore
    public String getPasswordHash() { return passwordHash(); }
    public Set<String> getRoleCodes() { return roleCodes(); }
    public Map<String, PermissionEffect> getPermissionOverrides() { return permissionOverrides(); }

}
