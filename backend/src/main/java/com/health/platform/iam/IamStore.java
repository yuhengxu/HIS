package com.health.platform.iam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class IamStore {
    private final AtomicLong userId = new AtomicLong(10);
    private final Map<Long, UserRecord> users = new LinkedHashMap<>();
    private final Map<String, RoleRecord> roles = new LinkedHashMap<>();
    private final Map<String, PermissionRecord> permissions = new LinkedHashMap<>();

    public IamStore() {
        seedPermissions();
        seedRoles();
        seedUsers();
    }

    public Collection<UserRecord> users() { return users.values(); }
    public Collection<RoleRecord> roles() { return roles.values(); }
    public Collection<PermissionRecord> permissions() { return permissions.values(); }

    public Optional<UserRecord> findUser(long id) { return Optional.ofNullable(users.get(id)); }
    public Optional<UserRecord> findUserByUsername(String username) {
        return users.values().stream().filter(user -> user.username().equals(username)).findFirst();
    }
    public Optional<RoleRecord> findRole(String code) { return Optional.ofNullable(roles.get(code)); }
    public boolean hasRole(long userId, String roleCode) { return findUser(userId).map(u -> u.roleCodes().contains(roleCode)).orElse(false); }

    public UserRecord createUser(String username, String displayName, Long reportToUserId, String wecomUserId, String departmentName) {
        UserRecord user = new UserRecord(userId.incrementAndGet(), username, displayName);
        user.update(username, displayName, reportToUserId, wecomUserId, departmentName);
        user.setPasswordHash(PasswordHasher.sha256("qwer1234"));
        users.put(user.id(), user);
        return user;
    }

    public RoleRecord createRole(String code, String name, String description, Set<String> permissionCodes) {
        RoleRecord role = new RoleRecord(code, name, description);
        role.permissionCodes().addAll(permissionCodes == null ? Set.of() : permissionCodes);
        roles.put(code, role);
        return role;
    }

    private void seedPermissions() {
        List.of(
            new PermissionRecord("iam:user:read", "查看用户", "IAM"),
            new PermissionRecord("iam:user:create", "创建用户", "IAM"),
            new PermissionRecord("iam:user:update", "更新用户", "IAM"),
            new PermissionRecord("iam:user:disable", "禁用用户", "IAM"),
            new PermissionRecord("iam:role:read", "查看角色", "IAM"),
            new PermissionRecord("iam:role:write", "维护角色", "IAM"),
            new PermissionRecord("iam:permission:read", "查看权限", "IAM"),
            new PermissionRecord("iam:permission:write", "维护权限", "IAM"),
            new PermissionRecord("iam:user-permission:write", "用户单独授权", "IAM"),
            new PermissionRecord("oa:process:read", "查看流程", "OA"),
            new PermissionRecord("oa:process:write", "维护流程", "OA"),
            new PermissionRecord("oa:instance:create", "发起流程", "OA"),
            new PermissionRecord("oa:instance:read", "查看流程实例", "OA"),
            new PermissionRecord("oa:task:read", "查看待办", "OA"),
            new PermissionRecord("oa:task:approve", "审批任务", "OA"),
            new PermissionRecord("oa:task:urge", "催办任务", "OA"),
            new PermissionRecord("oa:reminder:config", "配置提醒", "OA"),
            new PermissionRecord("inventory:warehouse:read", "查看仓库", "Inventory"),
            new PermissionRecord("inventory:warehouse:write", "维护仓库", "Inventory"),
            new PermissionRecord("inventory:item:read", "查看物资", "Inventory"),
            new PermissionRecord("inventory:item:write", "维护物资", "Inventory"),
            new PermissionRecord("inventory:price:read", "查看价格", "Inventory"),
            new PermissionRecord("inventory:price:write", "维护价格", "Inventory"),
            new PermissionRecord("inventory:inbound:create", "发起入库", "Inventory"),
            new PermissionRecord("inventory:inbound:approve", "审批入库", "Inventory"),
            new PermissionRecord("inventory:outbound:create", "发起出库", "Inventory"),
            new PermissionRecord("inventory:outbound:approve", "审批出库", "Inventory"),
            new PermissionRecord("inventory:stock:read", "查看库存", "Inventory"),
            new PermissionRecord("inventory:stock:write", "维护库存", "Inventory"),
            new PermissionRecord("audit:read", "查看审计", "Audit"),
            new PermissionRecord("ai-access:read", "查看 AI 接入", "AI Access")
        ).forEach(permission -> permissions.put(permission.code(), permission));
    }

    private void seedRoles() {
        createRole("SYSTEM_ADMIN", "系统管理员", "管理全部配置", permissions.keySet());
        createRole("OA_ADMIN", "OA 管理员", "管理 OA", Set.of("oa:process:read", "oa:process:write", "oa:instance:read", "oa:task:read", "oa:reminder:config"));
        createRole("INVENTORY_ADMIN", "物资管理员", "管理物资", Set.of("inventory:warehouse:read", "inventory:warehouse:write", "inventory:item:read", "inventory:item:write", "inventory:price:read", "inventory:price:write", "inventory:inbound:approve", "inventory:outbound:approve", "inventory:stock:read", "inventory:stock:write", "oa:task:read", "oa:task:approve"));
        createRole("FINANCE_APPROVER", "财务审批人", "审批报销", Set.of("oa:task:read", "oa:task:approve", "oa:instance:read"));
        createRole("DEPARTMENT_MANAGER", "部门负责人", "上级审批", Set.of("oa:task:read", "oa:task:approve", "oa:instance:read"));
        createRole("EMPLOYEE", "普通员工", "发起和查看", Set.of("oa:instance:create", "oa:instance:read", "oa:task:urge", "inventory:item:read", "inventory:stock:read", "inventory:price:read"));
        createRole("AI_CALLER", "AI 调用方", "只读调用", Set.of("inventory:item:read", "inventory:stock:read", "inventory:price:read"));
    }

    private void seedUsers() {
        UserRecord admin = new UserRecord(1, "admin", "系统管理员");
        admin.roleCodes().add("SYSTEM_ADMIN");
        admin.setPasswordHash(PasswordHasher.sha256("1qaz@WSX"));
        users.put(admin.id(), admin);
        UserRecord manager = new UserRecord(2, "manager", "部门负责人");
        manager.roleCodes().add("DEPARTMENT_MANAGER");
        manager.setPasswordHash(PasswordHasher.sha256("qwer1234"));
        users.put(manager.id(), manager);
        UserRecord employee = new UserRecord(3, "employee", "普通员工");
        employee.roleCodes().add("EMPLOYEE");
        employee.update(null, null, 2L, "employee.wecom", "综合科");
        employee.setPasswordHash(PasswordHasher.sha256("qwer1234"));
        users.put(employee.id(), employee);
        UserRecord inventoryAdmin = new UserRecord(4, "inventory", "物资管理员");
        inventoryAdmin.roleCodes().add("INVENTORY_ADMIN");
        inventoryAdmin.setPasswordHash(PasswordHasher.sha256("qwer1234"));
        users.put(inventoryAdmin.id(), inventoryAdmin);
        UserRecord finance = new UserRecord(5, "finance", "财务审批人");
        finance.roleCodes().add("FINANCE_APPROVER");
        finance.setPasswordHash(PasswordHasher.sha256("qwer1234"));
        users.put(finance.id(), finance);
    }
}
