package com.health.platform.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryPermissionTest {
    private InventoryStockService service;

    @BeforeEach
    void setUp() {
        IamStore iamStore = new IamStore();
        PermissionService permissionService = new PermissionService(iamStore);
        service = new InventoryStockService(new InventoryStore(), permissionService, new AuditService(), iamStore);
    }

    @Test
    void employeeCannotCreateItem() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createItem(3,
            new InventoryStockService.ItemRequest("X1", "测试物资", "non_medical", "个", null)));
        assertTrue(ex.getMessage().contains("INVENTORY_ADMIN") || ex.getMessage().contains("SYSTEM_ADMIN"));
    }

    @Test
    void inventoryAdminCanCreateItem() {
        service.createItem(4, new InventoryStockService.ItemRequest("X2", "管理员物资", "non_medical", "个", null));
    }

    @Test
    void ordinaryEmployeeCannotReadInventoryBackOffice() {
        assertThrows(BusinessException.class, () -> service.items(3));
        assertThrows(BusinessException.class, () -> service.stockViews(3, ""));
        assertThrows(BusinessException.class, () -> service.transactions(3));
    }

    @Test
    void onlySystemAdminCanCreateSpecialItem() {
        assertThrows(BusinessException.class, () -> service.createItem(4,
            new InventoryStockService.ItemRequest("S1", "特殊物资", "medical", "个", null, "SPECIAL")));
        service.createItem(1, new InventoryStockService.ItemRequest("S2", "特殊物资", "medical", "个", null, "SPECIAL"));
        assertTrue(service.items(4).stream().noneMatch(item -> "SPECIAL".equals(item.materialTag())));
        assertTrue(service.items(1).stream().anyMatch(item -> "SPECIAL".equals(item.materialTag())));
    }
}
