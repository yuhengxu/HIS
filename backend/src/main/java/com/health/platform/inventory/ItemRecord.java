package com.health.platform.inventory;

import java.math.BigDecimal;

public class ItemRecord {
    private final long id;
    private final String code;
    private final String name;
    private final String itemType;
    private final String unit;
    private BigDecimal latestPrice;

    public ItemRecord(long id, String code, String name, String itemType, String unit, BigDecimal latestPrice) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.itemType = itemType;
        this.unit = unit;
        this.latestPrice = latestPrice;
    }

    public long id() { return id; }
    public String code() { return code; }
    public String name() { return name; }
    public String itemType() { return itemType; }
    public String unit() { return unit; }
    public BigDecimal latestPrice() { return latestPrice; }
    public void setLatestPrice(BigDecimal latestPrice) { this.latestPrice = latestPrice; }
    public long getId() { return id(); }
    public String getCode() { return code(); }
    public String getName() { return name(); }
    public String getItemType() { return itemType(); }
    public String getUnit() { return unit(); }
    public BigDecimal getLatestPrice() { return latestPrice(); }

}
