package com.health.platform.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class InventoryStore {
    private final AtomicLong warehouseId = new AtomicLong(0);
    private final AtomicLong itemId = new AtomicLong(0);
    private final AtomicLong stockId = new AtomicLong(0);
    private final AtomicLong priceId = new AtomicLong(0);
    private final AtomicLong txnId = new AtomicLong(0);
    private final AtomicLong orderId = new AtomicLong(0);
    private final AtomicLong itemImageId = new AtomicLong(0);
    private final AtomicLong stockImageId = new AtomicLong(0);
    private final Map<Long, WarehouseRecord> warehouses = new LinkedHashMap<>();
    private final Map<Long, ItemRecord> items = new LinkedHashMap<>();
    private final Map<String, StockRecord> stocks = new LinkedHashMap<>();
    private final Map<Long, ItemImageRecord> itemImages = new LinkedHashMap<>();
    private final Map<Long, StockImageRecord> stockImages = new LinkedHashMap<>();
    private final List<PriceRecord> prices = new ArrayList<>();
    private final List<StockTxnRecord> transactions = new ArrayList<>();
    private final List<Map<String, Object>> inboundOrders = new ArrayList<>();
    private final List<Map<String, Object>> outboundOrders = new ArrayList<>();
    private final List<Map<String, Object>> reimbursements = new ArrayList<>();

    public InventoryStore() {
        WarehouseRecord medical = addWarehouse("MEDICAL_MAIN", "医疗用品库", "medical");
        WarehouseRecord nonMedical = addWarehouse("NON_MEDICAL_MAIN", "非医疗用品库", "non_medical");
        ItemRecord mask = addItem("MASK", "医用口罩", "medical", "只", new BigDecimal("0.50"));
        ItemRecord paper = addItem("PAPER", "办公用纸", "non_medical", "包", new BigDecimal("25.00"));
        setStock(medical.id(), mask.id(), new BigDecimal("100"));
        setStock(nonMedical.id(), paper.id(), new BigDecimal("20"));
    }

    public Collection<WarehouseRecord> warehouses() { return warehouses.values(); }
    public Collection<ItemRecord> items() { return items.values(); }
    public List<PriceRecord> prices() { return List.copyOf(prices); }
    public List<StockTxnRecord> transactions() { return List.copyOf(transactions); }
    public List<Map<String, Object>> inboundOrders() { return List.copyOf(inboundOrders); }
    public List<Map<String, Object>> outboundOrders() { return List.copyOf(outboundOrders); }
    public List<Map<String, Object>> reimbursements() { return List.copyOf(reimbursements); }

    public WarehouseRecord addWarehouse(String code, String name, String warehouseType) {
        WarehouseRecord warehouse = new WarehouseRecord(warehouseId.incrementAndGet(), code, name, warehouseType);
        warehouses.put(warehouse.id(), warehouse);
        return warehouse;
    }

    public ItemRecord addItem(String code, String name, String itemType, String unit, BigDecimal latestPrice) {
        ItemRecord item = new ItemRecord(itemId.incrementAndGet(), code, name, itemType, unit, latestPrice == null ? BigDecimal.ZERO : latestPrice);
        items.put(item.id(), item);
        prices.add(new PriceRecord(priceId.incrementAndGet(), item.id(), "reference", item.latestPrice(), OffsetDateTime.now()));
        return item;
    }

    public ItemRecord addItem(String code, String name, String itemType, String unit, BigDecimal latestPrice, String materialTag) {
        ItemRecord item = addItem(code, name, itemType, unit, latestPrice);
        item.setMaterialTag(materialTag);
        return item;
    }

    public List<ItemRecord> searchItems(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isBlank()) {
            return List.copyOf(items.values());
        }
        return items.values().stream()
            .filter(item -> item.code().toLowerCase().contains(normalized) || item.name().toLowerCase().contains(normalized))
            .toList();
    }

    public List<ItemRecord> searchClaimableItems(long warehouseId, String keyword) {
        WarehouseRecord warehouse = findWarehouse(warehouseId);
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return items.values().stream()
            .filter(item -> matchesWarehouseType(warehouse, item))
            .filter(item -> totalQuantity(warehouseId, item.id()).compareTo(BigDecimal.ZERO) > 0)
            .filter(item -> normalized.isBlank() || item.code().toLowerCase().contains(normalized) || item.name().toLowerCase().contains(normalized))
            .toList();
    }

    public ItemRecord findItem(long itemId) {
        ItemRecord item = items.get(itemId);
        if (item == null) {
            throw new IllegalStateException("Item not found");
        }
        return item;
    }

    public WarehouseRecord findWarehouse(long warehouseId) {
        WarehouseRecord warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            throw new IllegalStateException("Warehouse not found");
        }
        return warehouse;
    }

    public ItemRecord findByNameAndSpec(String name, String specification) {
        return items.values().stream()
            .filter(item -> item.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public void addItemImage(long itemId, long attachmentId, boolean primary) {
        ItemRecord item = findItem(itemId);
        String usageType = primary ? "main_image" : "gallery";
        int sortOrder = primary ? 0 : (int) itemImages.values().stream().filter(img -> img.itemId() == itemId && img.deletedAt() == null).count() + 1;
        if (primary) {
            itemImages.values().stream()
                .filter(img -> img.itemId() == itemId && img.deletedAt() == null && "main_image".equals(img.usageType()))
                .forEach(img -> img.setUsageType("gallery"));
        }
        ItemImageRecord image = new ItemImageRecord(itemImageId.incrementAndGet(), itemId, attachmentId, usageType, sortOrder);
        itemImages.put(image.id(), image);
        item.imageAttachmentIds().add(attachmentId);
        if (primary) {
            item.setPrimaryImageUrl(image.url());
        }
    }

    public List<ItemImageRecord> listItemImages(long itemId) {
        return itemImages.values().stream().filter(img -> img.itemId() == itemId && img.deletedAt() == null).toList();
    }

    public void setMainItemImage(long itemId, long imageId) {
        ItemImageRecord target = itemImages.get(imageId);
        if (target == null || target.itemId() != itemId || target.deletedAt() != null) {
            throw new IllegalStateException("Item image not found");
        }
        itemImages.values().stream()
            .filter(img -> img.itemId() == itemId && img.deletedAt() == null && "main_image".equals(img.usageType()))
            .forEach(img -> img.setUsageType("gallery"));
        target.setUsageType("main_image");
        findItem(itemId).setPrimaryImageUrl(target.url());
    }

    public void deleteItemImage(long itemId, long imageId) {
        ItemImageRecord image = itemImages.get(imageId);
        if (image == null || image.itemId() != itemId) {
            throw new IllegalStateException("Item image not found");
        }
        image.softDelete();
        ItemRecord item = findItem(itemId);
        item.imageAttachmentIds().remove(image.attachmentId());
        if ("main_image".equals(image.usageType())) {
            item.setPrimaryImageUrl(null);
            itemImages.values().stream()
                .filter(img -> img.itemId() == itemId && img.deletedAt() == null)
                .findFirst()
                .ifPresent(img -> {
                    img.setUsageType("main_image");
                    item.setPrimaryImageUrl(img.url());
                });
        }
    }

    public StockRecord findStock(long stockId) {
        return stocks.values().stream().filter(s -> s.id() == stockId).findFirst()
            .orElseThrow(() -> new IllegalStateException("Stock not found"));
    }

    public StockImageRecord addStockImage(long stockId, long attachmentId, String usageType) {
        findStock(stockId);
        long count = stockImages.values().stream().filter(img -> img.stockId() == stockId && img.deletedAt() == null).count();
        if (count >= 20) {
            throw new IllegalStateException("Stock image limit reached");
        }
        StockImageRecord image = new StockImageRecord(stockImageId.incrementAndGet(), stockId, attachmentId, usageType == null ? "stock_scene" : usageType, (int) count + 1);
        stockImages.put(image.id(), image);
        return image;
    }

    public List<StockImageRecord> listStockImages(long stockId) {
        return stockImages.values().stream().filter(img -> img.stockId() == stockId && img.deletedAt() == null).toList();
    }

    public void deleteStockImage(long stockId, long imageId) {
        StockImageRecord image = stockImages.get(imageId);
        if (image == null || image.stockId() != stockId) {
            throw new IllegalStateException("Stock image not found");
        }
        image.softDelete();
    }

    public List<StockViewRecord> stockViews(long actorUserId, String keyword) {
        return searchStocks(actorUserId, keyword).stream().map(this::toStockView).toList();
    }

    private StockViewRecord toStockView(StockRecord stock) {
        ItemRecord item = items.get(stock.itemId());
        WarehouseRecord warehouse = warehouses.get(stock.warehouseId());
        return new StockViewRecord(
            stock.id(), stock.warehouseId(), stock.itemId(), stock.quantity(), stock.ownerUserId(), "",
            item == null ? "" : item.name(), item == null ? "" : item.code(),
            warehouse == null ? "" : warehouse.name(), warehouse == null ? "" : warehouse.warehouseType(),
            item == null ? null : item.primaryImageUrl(), listStockImages(stock.id()));
    }

    public List<StockRecord> stockRecords() {
        return List.copyOf(stocks.values());
    }

    public List<StockSummaryViewRecord> stockSummaries(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        Map<String, BigDecimal> quantities = new LinkedHashMap<>();
        for (StockRecord stock : stocks.values()) {
            String summaryKey = stock.warehouseId() + ":" + stock.itemId();
            quantities.put(summaryKey, quantities.getOrDefault(summaryKey, BigDecimal.ZERO).add(stock.quantity()));
        }
        return quantities.entrySet().stream()
            .map(entry -> toStockSummary(entry.getKey(), entry.getValue()))
            .filter(summary -> summary.quantity().compareTo(BigDecimal.ZERO) > 0)
            .filter(summary -> normalized.isBlank() || matchesSummary(summary, normalized))
            .sorted(Comparator.comparingLong(StockSummaryViewRecord::warehouseId).thenComparingLong(StockSummaryViewRecord::itemId))
            .toList();
    }

    public List<StockRecord> searchStocks(long actorUserId, String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return stocks.values().stream()
            .filter(stock -> stock.ownerUserId() == actorUserId)
            .filter(stock -> normalized.isBlank() || matchesStock(stock, normalized))
            .toList();
    }

    public List<StockRecord> searchStocks(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return stocks.values().stream()
            .filter(stock -> matchesStock(stock, normalized))
            .toList();
    }

    public synchronized StockRecord createStock(long actorUserId, long warehouseId, long itemId, BigDecimal quantity) {
        assertWarehouseAndItemExist(warehouseId, itemId);
        String key = key(actorUserId, warehouseId, itemId);
        if (stocks.containsKey(key)) {
            throw new IllegalStateException("Stock already exists for warehouse and item");
        }
        StockRecord stock = new StockRecord(stockId.incrementAndGet(), warehouseId, itemId, quantity == null ? BigDecimal.ZERO : quantity, actorUserId);
        stocks.put(key, stock);
        return stock;
    }

    public synchronized StockRecord adjustStock(long actorUserId, long warehouseId, long itemId, BigDecimal newQuantity, String reason) {
        assertWarehouseAndItemExist(warehouseId, itemId);
        String stockKey = key(actorUserId, warehouseId, itemId);
        StockRecord current = stocks.get(stockKey);
        BigDecimal currentQuantity = current == null ? BigDecimal.ZERO : current.quantity();
        BigDecimal targetQuantity = newQuantity == null ? BigDecimal.ZERO : newQuantity;
        BigDecimal delta = targetQuantity.subtract(currentQuantity);
        StockRecord next = new StockRecord(current == null ? stockId.incrementAndGet() : current.id(), warehouseId, itemId, targetQuantity, actorUserId);
        stocks.put(stockKey, next);
        transactions.add(new StockTxnRecord(txnId.incrementAndGet(), "ADJUST", reason == null || reason.isBlank() ? "ADMIN-ADJUST" : reason, warehouseId, itemId, delta, actorUserId, OffsetDateTime.now()));
        return next;
    }

    public synchronized void inbound(long oaInstanceId, long actorUserId, long warehouseId, long itemId, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal amount = quantity.multiply(unitPrice);
        addStock(actorUserId, warehouseId, itemId, quantity);
        items.get(itemId).setLatestPrice(unitPrice);
        prices.add(new PriceRecord(priceId.incrementAndGet(), itemId, "purchase", unitPrice, OffsetDateTime.now()));
        transactions.add(new StockTxnRecord(txnId.incrementAndGet(), "INBOUND", "OA-" + oaInstanceId, warehouseId, itemId, quantity, actorUserId, OffsetDateTime.now()));
        inboundOrders.add(order("IN-" + orderId.incrementAndGet(), oaInstanceId, actorUserId, warehouseId, itemId, quantity, unitPrice, amount));
    }

    public synchronized void outbound(long oaInstanceId, long actorUserId, long warehouseId, long itemId, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal current = totalQuantity(warehouseId, itemId);
        if (current.subtract(quantity).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Stock cannot be negative");
        }
        deductStockDetails(warehouseId, itemId, quantity);
        BigDecimal amount = quantity.multiply(unitPrice);
        transactions.add(new StockTxnRecord(txnId.incrementAndGet(), "OUTBOUND", "OA-" + oaInstanceId, warehouseId, itemId, quantity.negate(), actorUserId, OffsetDateTime.now()));
        outboundOrders.add(order("OUT-" + orderId.incrementAndGet(), oaInstanceId, actorUserId, warehouseId, itemId, quantity, unitPrice, amount));
    }

    public synchronized void reimburse(long oaInstanceId, long actorUserId, Long relatedInboundOrderId, BigDecimal amount) {
        Map<String, Object> reimbursement = new LinkedHashMap<>();
        reimbursement.put("oaInstanceId", oaInstanceId);
        reimbursement.put("relatedInboundOrderId", relatedInboundOrderId);
        reimbursement.put("applicantUserId", actorUserId);
        reimbursement.put("amount", amount);
        reimbursement.put("status", "approved");
        reimbursements.add(reimbursement);
        inboundOrders.stream()
            .filter(order -> relatedInboundOrderId != null && relatedInboundOrderId.equals(longValue(order.get("id"))))
            .findFirst()
            .ifPresent(order -> order.put("reimbursementLinked", true));
    }

    private void setStock(long warehouseId, long itemId, BigDecimal quantity) {
        stocks.put(key(1, warehouseId, itemId), new StockRecord(stockId.incrementAndGet(), warehouseId, itemId, quantity, 1));
    }

    private void addStock(long warehouseId, long itemId, BigDecimal delta) {
        addStock(1, warehouseId, itemId, delta);
    }

    private void addStock(long actorUserId, long warehouseId, long itemId, BigDecimal delta) {
        assertWarehouseAndItemExist(warehouseId, itemId);
        String stockKey = key(actorUserId, warehouseId, itemId);
        StockRecord current = stocks.get(stockKey);
        if (current == null) {
            stocks.put(stockKey, new StockRecord(stockId.incrementAndGet(), warehouseId, itemId, delta, actorUserId));
            return;
        }
        stocks.put(stockKey, new StockRecord(current.id(), warehouseId, itemId, current.quantity().add(delta), actorUserId));
    }

    private void deductStockDetails(long warehouseId, long itemId, BigDecimal quantity) {
        BigDecimal remaining = quantity;
        List<StockRecord> details = stocks.values().stream()
            .filter(stock -> stock.warehouseId() == warehouseId && stock.itemId() == itemId)
            .filter(stock -> stock.quantity().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparingLong(StockRecord::id))
            .toList();
        for (StockRecord detail : details) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) return;
            BigDecimal deducted = detail.quantity().min(remaining);
            stocks.put(key(detail.ownerUserId(), warehouseId, itemId), new StockRecord(detail.id(), warehouseId, itemId, detail.quantity().subtract(deducted), detail.ownerUserId()));
            remaining = remaining.subtract(deducted);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Stock cannot be negative");
        }
    }

    private BigDecimal totalQuantity(long warehouseId, long itemId) {
        return stocks.values().stream()
            .filter(stock -> stock.warehouseId() == warehouseId && stock.itemId() == itemId)
            .map(StockRecord::quantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String key(long ownerUserId, long warehouseId, long itemId) {
        return ownerUserId + ":" + warehouseId + ":" + itemId;
    }

    private void assertWarehouseAndItemExist(long warehouseId, long itemId) {
        if (!warehouses.containsKey(warehouseId) || !items.containsKey(itemId)) {
            throw new IllegalStateException("Warehouse or item does not exist");
        }
    }

    private boolean matchesStock(StockRecord stock, String keyword) {
        WarehouseRecord warehouse = warehouses.get(stock.warehouseId());
        ItemRecord item = items.get(stock.itemId());
        return String.valueOf(stock.id()).contains(keyword)
            || String.valueOf(stock.warehouseId()).contains(keyword)
            || String.valueOf(stock.itemId()).contains(keyword)
            || (warehouse != null && contains(warehouse.code(), keyword))
            || (warehouse != null && contains(warehouse.name(), keyword))
            || (warehouse != null && contains(warehouse.warehouseType(), keyword))
            || (item != null && contains(item.code(), keyword))
            || (item != null && contains(item.name(), keyword))
            || (item != null && contains(item.itemType(), keyword));
    }

    private StockSummaryViewRecord toStockSummary(String summaryKey, BigDecimal quantity) {
        String[] parts = summaryKey.split(":");
        long warehouseId = Long.parseLong(parts[0]);
        long itemId = Long.parseLong(parts[1]);
        WarehouseRecord warehouse = warehouses.get(warehouseId);
        ItemRecord item = items.get(itemId);
        return new StockSummaryViewRecord(
            warehouseId,
            warehouse == null ? "" : warehouse.name(),
            warehouse == null ? "" : warehouse.warehouseType(),
            itemId,
            item == null ? "" : item.name(),
            item == null ? "" : item.code(),
            quantity);
    }

    private boolean matchesSummary(StockSummaryViewRecord summary, String keyword) {
        return String.valueOf(summary.warehouseId()).contains(keyword)
            || String.valueOf(summary.itemId()).contains(keyword)
            || contains(summary.warehouseName(), keyword)
            || contains(summary.warehouseType(), keyword)
            || contains(summary.itemCode(), keyword)
            || contains(summary.itemName(), keyword);
    }

    private boolean matchesWarehouseType(WarehouseRecord warehouse, ItemRecord item) {
        return "medical".equals(warehouse.warehouseType()) ? "medical".equals(item.itemType()) : !"medical".equals(item.itemType());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Map<String, Object> order(String orderNo, long oaInstanceId, long actorUserId, long warehouseId, long itemId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", orderId.get());
        order.put("orderNo", orderNo);
        order.put("oaInstanceId", oaInstanceId);
        order.put("warehouseId", warehouseId);
        order.put("itemId", itemId);
        order.put("createdBy", actorUserId);
        order.put("quantity", quantity);
        order.put("unitPrice", unitPrice);
        order.put("amount", amount);
        order.put("status", "approved");
        order.put("reimbursementLinked", false);
        return order;
    }

    public List<Map<String, Object>> unlinkedInboundOrders(long actorUserId) {
        return inboundOrders.stream()
            .filter(order -> actorUserId == longValue(order.get("createdBy")))
            .filter(order -> !Boolean.TRUE.equals(order.get("reimbursementLinked")))
            .toList();
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

}
