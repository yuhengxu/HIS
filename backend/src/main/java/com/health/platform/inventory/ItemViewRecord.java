package com.health.platform.inventory;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ItemViewRecord(
    long id,
    String code,
    String name,
    String itemType,
    String unit,
    BigDecimal latestPrice,
    @JsonInclude(JsonInclude.Include.NON_NULL) String materialTag,
    String primaryImageUrl,
    List<Long> imageAttachmentIds
) {
}
