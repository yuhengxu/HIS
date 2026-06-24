package com.health.platform.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockTxnRecord(long id, String txnType, String bizNo, long warehouseId, long itemId, BigDecimal quantityDelta, long operatorUserId, OffsetDateTime occurredAt) {
}
