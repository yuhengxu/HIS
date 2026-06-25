package com.health.platform.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.PermissionEffect;
import org.springframework.stereotype.Component;

@Component
public class MenuStore {
    private final AtomicLong menuId = new AtomicLong(0);
    private final Map<Long, MenuRecord> menus = new LinkedHashMap<>();
    private final Map<String, Set<Long>> roleMenus = new LinkedHashMap<>();
    private final Map<Long, Map<Long, PermissionEffect>> userMenuOverrides = new LinkedHashMap<>();

    public MenuStore() {
        seedMenus();
        seedRoleMenus();
    }

    public Collection<MenuRecord> menus() { return menus.values(); }

    public Optional<MenuRecord> find(long id) { return Optional.ofNullable(menus.get(id)); }

    public MenuRecord create(String code, String name, Long parentId, String path, String icon, int sortOrder) {
        if (menus.values().stream().anyMatch(m -> m.code().equals(code))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Menu code already exists");
        }
        MenuRecord menu = new MenuRecord(menuId.incrementAndGet(), parentId, code, name, path, icon, sortOrder);
        menus.put(menu.id(), menu);
        return menu;
    }

    public void delete(long id) {
        if (menus.values().stream().anyMatch(m -> id == (m.parentId() == null ? -1 : m.parentId()))) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Menu has children");
        }
        menus.remove(id);
        roleMenus.values().forEach(set -> set.remove(id));
    }

    public Set<Long> roleMenuIds(String roleCode) {
        return roleMenus.getOrDefault(roleCode, Set.of());
    }

    public void saveRoleMenus(String roleCode, Set<Long> menuIds) {
        roleMenus.put(roleCode, menuIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(menuIds));
    }

    public Map<Long, PermissionEffect> userMenuOverrides(long userId) {
        return userMenuOverrides.computeIfAbsent(userId, id -> new LinkedHashMap<>());
    }

    public List<MenuTreeNode> buildTree(Collection<Long> visibleMenuIds) {
        Map<Long, MenuTreeNode> nodes = new LinkedHashMap<>();
        for (MenuRecord menu : menus.values()) {
            if (!visibleMenuIds.contains(menu.id()) || !"enabled".equals(menu.status())) continue;
            nodes.put(menu.id(), new MenuTreeNode(menu));
        }
        List<MenuTreeNode> roots = new ArrayList<>();
        for (MenuTreeNode node : nodes.values()) {
            Long parentId = node.menu().parentId();
            if (parentId == null || !nodes.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodes.get(parentId).children().add(node);
            }
        }
        roots.sort(Comparator.comparingInt(n -> n.menu().sortOrder()));
        roots.forEach(this::sortChildren);
        return roots;
    }

    private void sortChildren(MenuTreeNode node) {
        node.children().sort(Comparator.comparingInt(n -> n.menu().sortOrder()));
        node.children().forEach(this::sortChildren);
    }

    private void seedMenus() {
        create("dashboard", "工作台", null, "/", "DataLine", 1);
        MenuRecord management = create("category.management", "管理类", null, "", "Setting", 10);
        create("iam.users", "用户管理", management.id(), "/iam/users", "User", 11);
        create("iam.roles", "角色权限", management.id(), "/iam/roles", "User", 12);
        create("system.menus", "菜单管理", management.id(), "/system/menus", "Menu", 13);
        MenuRecord oa = create("category.oa", "OA 类", null, "", "Document", 20);
        create("oa.start", "发起流程", oa.id(), "/oa/start", "Document", 21);
        create("oa.processes", "流程定义", oa.id(), "/oa/processes", "OfficeBuilding", 22);
        MenuRecord inventory = create("category.inventory", "库存类", null, "", "Box", 30);
        create("inventory.items", "物资档案", inventory.id(), "/inventory/items", "Box", 31);
        create("inventory.stocks", "库存查询", inventory.id(), "/inventory/stocks", "Box", 32);
        create("inventory.stockTxns", "库存流水", inventory.id(), "/inventory/stock-transactions", "Tickets", 33);
    }

    private void seedRoleMenus() {
        Map<String, Long> byCode = menus.values().stream().collect(Collectors.toMap(MenuRecord::code, MenuRecord::id));
        saveRoleMenus("SYSTEM_ADMIN", Set.of(
            byCode.get("dashboard"), byCode.get("category.management"), byCode.get("iam.users"), byCode.get("iam.roles"), byCode.get("system.menus"),
            byCode.get("category.oa"), byCode.get("oa.start"), byCode.get("oa.processes"),
            byCode.get("category.inventory"), byCode.get("inventory.items"), byCode.get("inventory.stocks"), byCode.get("inventory.stockTxns")));
        saveRoleMenus("OA_ADMIN", Set.of(byCode.get("dashboard"), byCode.get("category.oa"), byCode.get("oa.start"), byCode.get("oa.processes")));
        saveRoleMenus("INVENTORY_ADMIN", Set.of(byCode.get("dashboard"), byCode.get("category.oa"), byCode.get("oa.start"), byCode.get("category.inventory"), byCode.get("inventory.items"), byCode.get("inventory.stocks"), byCode.get("inventory.stockTxns")));
        saveRoleMenus("EMPLOYEE", Set.of(byCode.get("dashboard"), byCode.get("category.oa"), byCode.get("oa.start")));
        saveRoleMenus("FINANCE_APPROVER", Set.of(byCode.get("dashboard")));
        saveRoleMenus("DEPARTMENT_MANAGER", Set.of(byCode.get("dashboard")));
    }

    public static class MenuTreeNode {
        private final MenuRecord menu;
        private final List<MenuTreeNode> children = new ArrayList<>();

        public MenuTreeNode(MenuRecord menu) { this.menu = menu; }
        public MenuRecord menu() { return menu; }
        public List<MenuTreeNode> children() { return children; }
        public MenuRecord getMenu() { return menu; }
        public List<MenuTreeNode> getChildren() { return children; }
    }
}
