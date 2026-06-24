package com.health.platform.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceRecord(long id, long itemId, String priceType, BigDecimal price, OffsetDateTime effectiveAt) {
}
