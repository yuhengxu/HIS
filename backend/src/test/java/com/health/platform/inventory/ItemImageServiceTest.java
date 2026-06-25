package com.health.platform.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemImageServiceTest {
    private InventoryStockService service;
    private InventoryStore store;

    @BeforeEach
    void setUp() {
        store = new InventoryStore();
        IamStore iamStore = new IamStore();
        service = new InventoryStockService(store, new PermissionService(iamStore), new AuditService(), iamStore);
    }

    @Test
    void inventoryAdminCanManageItemImages() {
        service.addItemImage(4, 1, 1001, true);
        assertEquals(1, service.listItemImages(4, 1).size());
        service.addItemImage(4, 1, 1002, false);
        assertEquals(2, service.listItemImages(4, 1).size());
    }

    @Test
    void employeeCannotUploadItemImage() {
        assertThrows(BusinessException.class, () -> service.addItemImage(3, 1, 1001, true));
    }

    @Test
    void stockViewIncludesPrimaryImage() {
        service.createStock(4, new InventoryStockService.StockRequest(1, 1, BigDecimal.ONE));
        var views = service.stockViews(4, "");
        assertFalse(views.isEmpty());
    }
}
