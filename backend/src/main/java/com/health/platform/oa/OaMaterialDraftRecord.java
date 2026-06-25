package com.health.platform.oa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OaMaterialDraftRecord {
    private final long id;
    private final long instanceId;
    private String name;
    private String category;
    private String itemType;
    private String specification;
    private String unit;
    private BigDecimal defaultPrice;
    private String supplier;
    private String createReason;
    private final List<Long> imageAttachmentIds = new ArrayList<>();
    private Long resolvedItemId;

    public OaMaterialDraftRecord(long id, long instanceId, String name, String category, String itemType,
                                 String specification, String unit, BigDecimal defaultPrice, String supplier, String createReason) {
        this.id = id;
        this.instanceId = instanceId;
        this.name = name;
        this.category = category;
        this.itemType = itemType;
        this.specification = specification;
        this.unit = unit;
        this.defaultPrice = defaultPrice;
        this.supplier = supplier;
        this.createReason = createReason;
    }

    public long id() { return id; }
    public long instanceId() { return instanceId; }
    public String name() { return name; }
    public String category() { return category; }
    public String itemType() { return itemType; }
    public String specification() { return specification; }
    public String unit() { return unit; }
    public BigDecimal defaultPrice() { return defaultPrice; }
    public String supplier() { return supplier; }
    public String createReason() { return createReason; }
    public List<Long> imageAttachmentIds() { return imageAttachmentIds; }
    public Long resolvedItemId() { return resolvedItemId; }

    public void resolveItem(long itemId) { this.resolvedItemId = itemId; }

    public long getId() { return id(); }
    public long getInstanceId() { return instanceId(); }
    public String getName() { return name(); }
    public String getCategory() { return category(); }
    public String getItemType() { return itemType(); }
    public String getSpecification() { return specification(); }
    public String getUnit() { return unit(); }
    public BigDecimal getDefaultPrice() { return defaultPrice(); }
    public String getSupplier() { return supplier(); }
    public String getCreateReason() { return createReason(); }
    public List<Long> getImageAttachmentIds() { return imageAttachmentIds(); }
    public Long getResolvedItemId() { return resolvedItemId(); }
}
