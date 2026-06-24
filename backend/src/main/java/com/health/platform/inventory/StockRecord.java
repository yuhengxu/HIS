package com.health.platform.inventory;

import java.math.BigDecimal;

public record StockRecord(long id, long warehouseId, long itemId, BigDecimal quantity) {
}
