package com.health.platform.system;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MenuRecord {
    private final long id;
    private Long parentId;
    private final String code;
    private String name;
    private String clientType = "web_admin";
    private String path;
    private String component;
    private String icon;
    private int sortOrder;
    private boolean visible = true;
    private String status = "enabled";
    private final Set<String> permissionCodes = new LinkedHashSet<>();

    public MenuRecord(long id, Long parentId, String code, String name, String path, String icon, int sortOrder) {
        this.id = id;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.path = path;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    public long id() { return id; }
    public Long parentId() { return parentId; }
    public String code() { return code; }
    public String name() { return name; }
    public String clientType() { return clientType; }
    public String path() { return path; }
    public String component() { return component; }
    public String icon() { return icon; }
    public int sortOrder() { return sortOrder; }
    public boolean visible() { return visible; }
    public String status() { return status; }
    public Set<String> permissionCodes() { return permissionCodes; }

    public void update(MenuRequest request) {
        if (request.name() != null) this.name = request.name();
        if (request.parentId() != null) this.parentId = request.parentId();
        if (request.path() != null) this.path = request.path();
        if (request.component() != null) this.component = request.component();
        if (request.icon() != null) this.icon = request.icon;
        if (request.sortOrder() != null) this.sortOrder = request.sortOrder();
        if (request.visible() != null) this.visible = request.visible();
        if (request.status() != null) this.status = request.status();
        if (request.permissionCodes() != null) {
            permissionCodes.clear();
            permissionCodes.addAll(request.permissionCodes());
        }
    }

    public record MenuRequest(String name, Long parentId, String path, String component, String icon, Integer sortOrder, Boolean visible, String status, Set<String> permissionCodes) {
    }

    public long getId() { return id(); }
    public Long getParentId() { return parentId(); }
    public String getCode() { return code(); }
    public String getName() { return name(); }
    public String getPath() { return path(); }
    public String getIcon() { return icon(); }
    public int getSortOrder() { return sortOrder(); }
    public boolean isVisible() { return visible(); }
    public String getStatus() { return status(); }
    public Set<String> getPermissionCodes() { return permissionCodes(); }
    public List<MenuRecord> getChildren() { return new ArrayList<>(); }
}
