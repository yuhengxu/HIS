package com.health.platform.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import com.health.platform.audit.AuditService;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import org.junit.jupiter.api.Test;

class InventoryStockServiceTest {
    @Test
    void outboundCannotMakeStockNegative() {
        InventoryStore store = new InventoryStore();
        assertThrows(IllegalStateException.class, () -> store.outbound(1, 3, 1, 1, new BigDecimal("1000"), BigDecimal.ONE));
    }

    @Test
    void outboundConsumesAggregateStockAcrossInboundOwners() {
        InventoryStore store = new InventoryStore();
        store.createStock(3, 1, 2, new BigDecimal("10"));
        store.createStock(4, 1, 2, new BigDecimal("20"));

        store.outbound(20, 5, 1, 2, new BigDecimal("25"), BigDecimal.ONE);

        assertEquals(1, store.searchStocks(4, "办公").size());
        assertEquals(new BigDecimal("5"), store.searchStocks(4, "办公").get(0).quantity());
        assertEquals(0, store.searchStocks(3, "办公").get(0).quantity().compareTo(BigDecimal.ZERO));
    }

    @Test
    void claimableItemsAreFilteredByWarehouseType() {
        InventoryStore store = new InventoryStore();

        assertTrue(store.searchClaimableItems(1, "").stream().anyMatch(item -> "医用口罩".equals(item.name())));
        assertTrue(store.searchClaimableItems(1, "").stream().noneMatch(item -> "办公用纸".equals(item.name())));
        assertTrue(store.searchClaimableItems(2, "").stream().anyMatch(item -> "办公用纸".equals(item.name())));
    }

    @Test
    void inboundItemsAreFilteredByWarehouseTypeWithoutStockRequirement() {
        InventoryStore store = new InventoryStore();
        store.addItem("BED", "护理床", "non_medical", "张", new BigDecimal("800.00"));

        assertTrue(store.searchItemsByWarehouseType(1, "").stream().noneMatch(item -> "办公用纸".equals(item.name())));
        assertTrue(store.searchItemsByWarehouseType(2, "").stream().noneMatch(item -> "医用口罩".equals(item.name())));
        assertTrue(store.searchItemsByWarehouseType(2, "").stream().anyMatch(item -> "护理床".equals(item.name())));
    }

    @Test
    void stockIdAutoIncrementsAndSearchMatchesItemName() {
        InventoryStore store = new InventoryStore();
        StockRecord stock = store.createStock(3, 1, 2, new BigDecimal("8"));

        assertEquals(3, stock.id());
        assertEquals(2, store.searchStocks("办公").size());
        assertEquals(1, store.searchStocks(3, "办公").size());
        assertThrows(IllegalStateException.class, () -> store.createStock(3, 1, 2, BigDecimal.ONE));
    }

    @Test
    void stockAndTransactionsAreScopedToOwnerExceptSystemAdmin() {
        IamStore iamStore = new IamStore();
        InventoryStore store = new InventoryStore();
        InventoryStockService service = new InventoryStockService(store, new PermissionService(iamStore), new AuditService(), iamStore);
        store.inbound(10, 3, 1, 1, new BigDecimal("5"), new BigDecimal("0.60"));
        store.inbound(11, 4, 2, 2, new BigDecimal("3"), new BigDecimal("25.00"));

        assertTrue(service.stockViews(1, "").size() >= 4);
        assertThrows(RuntimeException.class, () -> service.stockViews(3, ""));
        assertEquals(1, service.stockViews(4, "").stream().filter(stock -> stock.getOwnerUserId() == 4).count());
        assertTrue(service.stockViews(4, "").stream().anyMatch(stock -> "物资管理员".equals(stock.getOwnerName())));
        assertEquals(2, service.transactions(4).size());
        assertEquals(2, service.transactions(1).size());
    }

    @Test
    void itemCanBeUpdatedWithoutChangingIdAndDeletedWhenUnreferenced() {
        IamStore iamStore = new IamStore();
        InventoryStore store = new InventoryStore();
        InventoryStockService service = new InventoryStockService(store, new PermissionService(iamStore), new AuditService(), iamStore);
        ItemRecord item = service.createItem(1, new InventoryStockService.ItemRequest("GLOVE", "手套", "medical", "盒", new BigDecimal("12.50")));

        ItemRecord updated = service.updateItem(1, item.id(), new InventoryStockService.ItemRequest("GLOVE-2", "检查手套", "medical", "包", new BigDecimal("15.00")));

        assertEquals(item.id(), updated.id());
        assertEquals("GLOVE-2", updated.code());
        assertEquals("检查手套", updated.name());
        service.deleteItem(1, item.id());
        assertThrows(RuntimeException.class, () -> service.getItem(1, item.id()));
    }

    @Test
    void itemWithStockCannotBeDeleted() {
        IamStore iamStore = new IamStore();
        InventoryStore store = new InventoryStore();
        InventoryStockService service = new InventoryStockService(store, new PermissionService(iamStore), new AuditService(), iamStore);

        assertThrows(RuntimeException.class, () -> service.deleteItem(1, 1));
    }

    @Test
    void systemAdminAdjustsStockAndCreatesTransaction() {
        IamStore iamStore = new IamStore();
        InventoryStore store = new InventoryStore();
        InventoryStockService service = new InventoryStockService(store, new PermissionService(iamStore), new AuditService(), iamStore);

        service.adjustStock(1, new InventoryStockService.StockAdjustRequest(1, 1, new BigDecimal("123"), "盘点调整"));

        assertEquals(new BigDecimal("123"), service.stockViews(1, "口罩").get(0).getQuantity());
        assertTrue(service.transactions(1).stream().anyMatch(txn -> "ADJUST".equals(txn.txnType())));
        assertThrows(RuntimeException.class, () -> service.adjustStock(4, new InventoryStockService.StockAdjustRequest(1, 1, BigDecimal.ONE, "越权")));
    }
}
