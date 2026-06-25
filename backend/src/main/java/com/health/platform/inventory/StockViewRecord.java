package com.health.platform.inventory;

import java.math.BigDecimal;
import java.util.List;

public class StockViewRecord {
    private final long id;
    private final long warehouseId;
    private final long itemId;
    private final BigDecimal quantity;
    private final long ownerUserId;
    private final String ownerName;
    private final String itemName;
    private final String itemCode;
    private final String warehouseName;
    private final String warehouseType;
    private final String primaryImageUrl;
    private final List<StockImageRecord> stockImages;

    public StockViewRecord(long id, long warehouseId, long itemId, BigDecimal quantity, long ownerUserId, String ownerName, String itemName, String itemCode, String warehouseName, String warehouseType, String primaryImageUrl, List<StockImageRecord> stockImages) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.ownerUserId = ownerUserId;
        this.ownerName = ownerName;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.warehouseName = warehouseName;
        this.warehouseType = warehouseType;
        this.primaryImageUrl = primaryImageUrl;
        this.stockImages = stockImages;
    }

    public long getId() { return id; }
    public long getWarehouseId() { return warehouseId; }
    public long getItemId() { return itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public long getOwnerUserId() { return ownerUserId; }
    public String getOwnerName() { return ownerName; }
    public String getItemName() { return itemName; }
    public String getItemCode() { return itemCode; }
    public String getWarehouseName() { return warehouseName; }
    public String getWarehouseType() { return warehouseType; }
    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public List<StockImageRecord> getStockImages() { return stockImages; }
}
