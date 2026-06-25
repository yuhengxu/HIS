package com.health.platform.inventory;

import java.time.OffsetDateTime;

public class StockImageRecord {
    private final long id;
    private final long stockId;
    private final long attachmentId;
    private String usageType;
    private int sortOrder;
    private final OffsetDateTime createdAt;
    private OffsetDateTime deletedAt;

    public StockImageRecord(long id, long stockId, long attachmentId, String usageType, int sortOrder) {
        this.id = id;
        this.stockId = stockId;
        this.attachmentId = attachmentId;
        this.usageType = usageType;
        this.sortOrder = sortOrder;
        this.createdAt = OffsetDateTime.now();
    }

    public long id() { return id; }
    public long stockId() { return stockId; }
    public long attachmentId() { return attachmentId; }
    public String usageType() { return usageType; }
    public int sortOrder() { return sortOrder; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime deletedAt() { return deletedAt; }
    public String url() { return "/api/v1/attachments/" + attachmentId + "/content"; }

    public void softDelete() { this.deletedAt = OffsetDateTime.now(); }

    public long getId() { return id(); }
    public long getStockId() { return stockId(); }
    public long getAttachmentId() { return attachmentId(); }
    public String getUsageType() { return usageType(); }
    public int getSortOrder() { return sortOrder(); }
    public String getUrl() { return url(); }
}
