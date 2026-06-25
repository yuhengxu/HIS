package com.health.platform.inventory;

import java.math.BigDecimal;

public record StockSummaryViewRecord(
    long warehouseId,
    String warehouseName,
    String warehouseType,
    long itemId,
    String itemName,
    String itemCode,
    BigDecimal quantity
) {
}
