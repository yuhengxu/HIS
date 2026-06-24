package com.health.platform.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InventoryStockServiceTest {
    @Test
    void outboundCannotMakeStockNegative() {
        InventoryStore store = new InventoryStore();
        assertThrows(IllegalStateException.class, () -> store.outbound(1, 3, 1, 1, new BigDecimal("1000"), BigDecimal.ONE));
    }

    @Test
    void stockIdAutoIncrementsAndSearchMatchesItemName() {
        InventoryStore store = new InventoryStore();
        StockRecord stock = store.createStock(1, 2, new BigDecimal("8"));

        assertEquals(3, stock.id());
        assertEquals(2, store.searchStocks("办公").size());
        assertThrows(IllegalStateException.class, () -> store.createStock(1, 2, BigDecimal.ONE));
    }
}
