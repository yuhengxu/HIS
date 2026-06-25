package com.health.platform.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockTxnViewRecord(
    long id,
    String txnType,
    String txnTypeName,
    String bizNo,
    long warehouseId,
    String warehouseName,
    long itemId,
    String itemName,
    String itemCode,
    BigDecimal quantityDelta,
    long operatorUserId,
    String operatorName,
    OffsetDateTime occurredAt
) {
}
