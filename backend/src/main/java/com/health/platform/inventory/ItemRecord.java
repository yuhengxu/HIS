package com.health.platform.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ItemRecord {
    private final long id;
    private String code;
    private String name;
    private String itemType;
    private String unit;
    private BigDecimal latestPrice;
    private String primaryImageUrl;
    private String materialTag = "NORMAL";
    private final Set<Long> imageAttachmentIds = new LinkedHashSet<>();

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
    public String primaryImageUrl() { return primaryImageUrl; }
    public String materialTag() { return materialTag; }
    public Set<Long> imageAttachmentIds() { return imageAttachmentIds; }

    public void update(String code, String name, String itemType, String unit, BigDecimal latestPrice) {
        if (code != null && !code.isBlank()) this.code = code;
        if (name != null && !name.isBlank()) this.name = name;
        if (itemType != null) this.itemType = itemType;
        if (unit != null) this.unit = unit;
        if (latestPrice != null) this.latestPrice = latestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) { this.latestPrice = latestPrice; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }
    public void setMaterialTag(String materialTag) { this.materialTag = materialTag == null || materialTag.isBlank() ? "NORMAL" : materialTag; }

    public long getId() { return id(); }
    public String getCode() { return code(); }
    public String getName() { return name(); }
    public String getItemType() { return itemType(); }
    public String getUnit() { return unit(); }
    public BigDecimal getLatestPrice() { return latestPrice(); }
    public String getPrimaryImageUrl() { return primaryImageUrl(); }
    public String getMaterialTag() { return materialTag(); }
    public List<Long> getImageAttachmentIds() { return new ArrayList<>(imageAttachmentIds()); }
}
