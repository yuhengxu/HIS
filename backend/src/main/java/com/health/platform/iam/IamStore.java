package com.health.platform.iam;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
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

    public Collection<UserRecord> users() {
        return users.values().stream().filter(u -> u.deletedAt() == null).toList();
    }

    public Collection<RoleRecord> roles() {
        return roles.values().stream().filter(r -> r.deletedAt() == null).toList();
    }

    public Collection<PermissionRecord> permissions() { return permissions.values(); }

    public Optional<UserRecord> findUser(long id) { return Optional.ofNullable(users.get(id)).filter(u -> u.deletedAt() == null); }
    public Optional<UserRecord> findUserByUsername(String username) {
        return users.values().stream()
            .filter(u -> u.deletedAt() == null && u.username().equalsIgnoreCase(username))
            .findFirst();
    }
    public Optional<RoleRecord> findRole(String code) {
        return Optional.ofNullable(roles.get(code)).filter(r -> r.deletedAt() == null);
    }
    public boolean hasRole(long userId, String roleCode) {
        return findUser(userId).map(u -> u.roleCodes().contains(roleCode)).orElse(false);
    }

    public long countUsersWithRole(String roleCode) {
        return users.values().stream()
            .filter(u -> u.deletedAt() == null && u.roleCodes().contains(roleCode))
            .count();
    }

    public UserRecord createUser(String username, String displayName, Long reportToUserId, String wecomUserId, String departmentName, Set<String> roleCodes) {
        if (findUserByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Username already exists");
        }
        UserRecord user = new UserRecord(userId.incrementAndGet(), username, displayName);
        user.update(username, displayName, reportToUserId, wecomUserId, departmentName);
        user.setPasswordHash(PasswordHasher.sha256("qwer1234"));
        user.setMustChangePassword(true);
        if (roleCodes != null) {
            user.roleCodes().addAll(roleCodes);
        }
        users.put(user.id(), user);
        return user;
    }

    public void softDeleteUser(long userId) {
        UserRecord user = users.get(userId);
        if (user != null) {
            user.softDelete();
            user.setEnabled(false);
        }
    }

    public RoleRecord createRole(String code, String name, String description, Set<String> permissionCodes, Integer sortOrder) {
        if (roles.containsKey(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Role code already exists");
        }
        RoleRecord role = new RoleRecord(code, name, description, false, sortOrder == null ? 100 : sortOrder);
        role.permissionCodes().addAll(permissionCodes == null ? Set.of() : permissionCodes);
        roles.put(code, role);
        return role;
    }

    public void softDeleteRole(String code) {
        RoleRecord role = roles.get(code);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Role not found");
        }
        if (role.systemBuiltIn()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "System built-in role cannot be deleted");
        }
        if (countUsersWithRole(code) > 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Role is bound to users, disable instead of delete");
        }
        role.softDelete();
    }

    private void seedPermissions() {
        List.of(
            perm("iam:user:read", "用户查看", "IAM", "user", "查看用户列表、用户详情、用户角色与单独授权", "SYSTEM_ADMIN"),
            perm("iam:user:create", "用户新增", "IAM", "user", "新建系统用户，设置基础信息、角色、汇报上级、企业微信 ID", "SYSTEM_ADMIN"),
            perm("iam:user:update", "用户修改", "IAM", "user", "修改用户资料、状态、角色、单独授权、汇报上级", "SYSTEM_ADMIN"),
            perm("iam:user:disable", "用户停用", "IAM", "user", "停用用户登录与业务操作权限，保留审计数据", "SYSTEM_ADMIN"),
            perm("iam:user:delete", "用户删除", "IAM", "user", "软删除用户，仅允许无未完结业务或满足归档条件时执行", "SYSTEM_ADMIN"),
            perm("iam:role:read", "角色查看", "IAM", "role", "查看角色列表、角色详情、已绑定权限", "SYSTEM_ADMIN"),
            perm("iam:role:create", "角色新增", "IAM", "role", "新增业务角色，设置角色编码、名称、说明和初始权限", "SYSTEM_ADMIN"),
            perm("iam:role:update", "角色修改", "IAM", "role", "修改角色名称、说明、状态、排序和权限绑定", "SYSTEM_ADMIN"),
            perm("iam:role:delete", "角色删除", "IAM", "role", "删除或禁用非系统内置角色；已有用户绑定时禁止硬删", "SYSTEM_ADMIN"),
            perm("iam:role:write", "角色维护", "IAM", "role", "兼容旧权限点，等同角色新增/修改/删除", "SYSTEM_ADMIN"),
            perm("iam:permission:read", "权限查看", "IAM", "permission", "查看权限点、权限域、权限说明，用于角色配置", "SYSTEM_ADMIN"),
            perm("iam:permission:write", "权限维护", "IAM", "permission", "新增、修改、启停权限点；系统内置权限不允许随意删除", "SYSTEM_ADMIN"),
            perm("iam:user-permission:write", "用户单独授权", "IAM", "user_permission", "对单个用户加授或撤销某个权限点", "SYSTEM_ADMIN"),
            perm("oa:process:read", "OA 流程定义查看", "OA", "process", "查看可发起流程和流程定义详情", "SYSTEM_ADMIN", "OA_ADMIN", "EMPLOYEE"),
            perm("oa:process:write", "OA 流程定义维护", "OA", "process", "新增、修改、启停 OA 流程定义和节点", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:process-definition:create", "流程定义新增", "OA", "process", "新增或复制 OA 流程定义", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:process-definition:update", "流程定义修改", "OA", "process", "修改流程定义基础信息", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:process-definition:delete", "流程定义删除", "OA", "process", "删除或停用流程定义", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:process-definition:publish", "流程定义发布", "OA", "process", "发布流程定义为可发起状态", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:process-node:write", "流程节点维护", "OA", "process", "保存流程审批节点配置", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:instance:create", "OA 流程发起", "OA", "instance", "在 OA 发起页选择具体流程并提交申请", "SYSTEM_ADMIN", "OA_ADMIN", "INVENTORY_ADMIN", "EMPLOYEE"),
            perm("oa:instance:read", "OA 实例查看", "OA", "instance", "查看本人发起、本人待办或授权范围内的 OA 实例", "SYSTEM_ADMIN", "OA_ADMIN", "INVENTORY_ADMIN", "EMPLOYEE"),
            perm("oa:task:read", "OA 待办查看", "OA", "task", "查看本人待办、已办、相关审批任务", "SYSTEM_ADMIN", "OA_ADMIN", "INVENTORY_ADMIN", "FINANCE_APPROVER", "DEPARTMENT_MANAGER", "EMPLOYEE"),
            perm("oa:task:approve", "OA 审批处理", "OA", "task", "对本人或本角色待办进行通过、驳回、转交等处理", "SYSTEM_ADMIN", "OA_ADMIN", "INVENTORY_ADMIN", "FINANCE_APPROVER", "DEPARTMENT_MANAGER"),
            perm("oa:task:urge", "OA 催办", "OA", "task", "发起人对当前处理人执行催办，受冷却时间限制", "SYSTEM_ADMIN", "OA_ADMIN", "EMPLOYEE"),
            perm("oa:reminder:config", "OA 提醒配置", "OA", "reminder", "配置流程或节点级提醒频率、冷却时间、机器人", "SYSTEM_ADMIN", "OA_ADMIN"),
            perm("oa:attachment:write", "OA 附件上传", "OA", "attachment", "在 OA 表单或审批过程中上传物资图片、凭证等附件", "SYSTEM_ADMIN", "OA_ADMIN", "INVENTORY_ADMIN", "EMPLOYEE"),
            perm("inventory:warehouse:read", "仓库查看", "Inventory", "warehouse", "查看仓库档案", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:warehouse:write", "仓库维护", "Inventory", "warehouse", "新增、修改、停用仓库档案", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:item:read", "物资档案查看", "Inventory", "item", "查看物资档案、图片、分类、规格、单位、价格摘要", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:item:write", "物资档案维护", "Inventory", "item", "新增、修改、删除物资档案和物资图片", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:price:read", "价格查看", "Inventory", "price", "查看物资价格记录", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:price:write", "价格维护", "Inventory", "price", "维护物资价格记录", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:inbound:create", "入库申请", "Inventory", "inbound", "发起物资入库 OA 或创建入库草稿", "SYSTEM_ADMIN", "INVENTORY_ADMIN", "EMPLOYEE"),
            perm("inventory:inbound:approve", "入库审批", "Inventory", "inbound", "入库 OA 审批通过后生成入库单和库存流水", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:outbound:create", "物品领用申请", "Inventory", "outbound", "发起物品领用流程或创建领用草稿", "SYSTEM_ADMIN", "INVENTORY_ADMIN", "EMPLOYEE"),
            perm("inventory:outbound:approve", "物品领用审批", "Inventory", "outbound", "物品领用审批通过后生成领用单和库存流水", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:stock:read", "库存查询", "Inventory", "stock", "查询库存余额、库存流水、物资图片", "SYSTEM_ADMIN", "INVENTORY_ADMIN", "AI_CALLER"),
            perm("inventory:stock:write", "库存调整", "Inventory", "stock", "执行库存调整、盘点差异处理", "SYSTEM_ADMIN"),
            perm("inventory:image:write", "物资图片维护", "Inventory", "image", "上传、替换、删除物资主图和附图", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:item:image:write", "物资图片维护", "Inventory", "image", "上传、替换、删除物资主图和附图", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("inventory:stock:image:write", "库存图片维护", "Inventory", "image", "上传、删除库存现场图", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("menu:read", "菜单查看", "IAM", "menu", "查看菜单树与角色/用户菜单绑定", "SYSTEM_ADMIN"),
            perm("menu:write", "菜单维护", "IAM", "menu", "新增、修改、删除菜单", "SYSTEM_ADMIN"),
            perm("menu:role-bind", "角色菜单绑定", "IAM", "menu", "为角色配置可见菜单", "SYSTEM_ADMIN"),
            perm("menu:user-bind", "用户菜单覆盖", "IAM", "menu", "为用户加授或撤销菜单", "SYSTEM_ADMIN"),
            perm("file:upload", "文件上传", "Audit", "file", "通用文件上传", "SYSTEM_ADMIN", "INVENTORY_ADMIN", "OA_ADMIN", "EMPLOYEE"),
            perm("file:read", "文件读取", "Audit", "file", "读取已授权文件", "SYSTEM_ADMIN", "INVENTORY_ADMIN", "OA_ADMIN", "EMPLOYEE"),
            perm("file:delete", "文件删除", "Audit", "file", "删除文件业务关联", "SYSTEM_ADMIN", "INVENTORY_ADMIN"),
            perm("finance:reimbursement:create", "报销申请", "Finance", "reimbursement", "发起报销 OA，填写金额、关联入库 OA、上传凭证", "SYSTEM_ADMIN", "EMPLOYEE"),
            perm("finance:reimbursement:approve", "报销审批", "Finance", "reimbursement", "处理报销审批节点，确认凭证与金额", "SYSTEM_ADMIN", "FINANCE_APPROVER"),
            perm("audit:read", "审计查看", "Audit", "audit", "查看用户、权限、物资、库存、OA、附件操作日志", "SYSTEM_ADMIN"),
            perm("ai-access:read", "AI 接入查看", "AI Access", "ai", "查看 AI 可调用接口白名单和调用审计", "SYSTEM_ADMIN", "AI_CALLER")
        ).forEach(permission -> permissions.put(permission.code(), permission));
    }

    private PermissionRecord perm(String code, String name, String domain, String resourceType, String description, String... defaultRoles) {
        return new PermissionRecord(code, name, domain, resourceType, description, true, defaultRoles);
    }

    private void seedRoles() {
        createBuiltInRole("SYSTEM_ADMIN", "系统管理员", "管理全部配置", 1, permissions.keySet());
        createBuiltInRole("OA_ADMIN", "OA 管理员", "管理 OA", 2, Set.of(
            "oa:process:read", "oa:process:write", "oa:process-definition:create", "oa:process-definition:update",
            "oa:process-definition:delete", "oa:process-definition:publish", "oa:process-node:write",
            "oa:instance:create", "oa:instance:read", "oa:task:read", "oa:reminder:config", "oa:attachment:write"));
        createBuiltInRole("INVENTORY_ADMIN", "物资管理员", "管理物资", 3, Set.of(
            "inventory:warehouse:read", "inventory:warehouse:write", "inventory:item:read", "inventory:item:write",
            "inventory:price:read", "inventory:price:write", "inventory:inbound:create", "inventory:inbound:approve",
            "inventory:outbound:create", "inventory:outbound:approve", "inventory:stock:read", "inventory:stock:write",
            "inventory:image:write", "inventory:item:image:write", "inventory:stock:image:write",
            "file:upload", "file:read", "file:delete",
            "oa:instance:create", "oa:instance:read", "oa:task:read", "oa:task:approve", "oa:attachment:write"));
        createBuiltInRole("FINANCE_APPROVER", "财务审批人", "审批报销", 4, Set.of(
            "oa:task:read", "oa:task:approve", "oa:instance:read", "finance:reimbursement:approve", "oa:attachment:write"));
        createBuiltInRole("DEPARTMENT_MANAGER", "部门负责人", "上级审批", 5, Set.of(
            "oa:task:read", "oa:task:approve", "oa:instance:read"));
        createBuiltInRole("EMPLOYEE", "普通员工", "发起和查看", 6, Set.of(
            "oa:process:read", "oa:instance:create", "oa:instance:read", "oa:task:read", "oa:task:urge",
            "inventory:inbound:create", "inventory:outbound:create", "finance:reimbursement:create", "oa:attachment:write"));
        createBuiltInRole("AI_CALLER", "AI 调用方", "只读调用", 7, Set.of(
            "inventory:item:read", "inventory:stock:read", "inventory:price:read", "ai-access:read"));
    }

    private void createBuiltInRole(String code, String name, String description, int sortOrder, Set<String> permissionCodes) {
        RoleRecord role = new RoleRecord(code, name, description, true, sortOrder);
        role.permissionCodes().addAll(permissionCodes);
        roles.put(code, role);
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
