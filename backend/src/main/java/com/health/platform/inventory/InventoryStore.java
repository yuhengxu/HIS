package com.health.platform.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
    private final Map<Long, WarehouseRecord> warehouses = new LinkedHashMap<>();
    private final Map<Long, ItemRecord> items = new LinkedHashMap<>();
    private final Map<String, StockRecord> stocks = new LinkedHashMap<>();
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

    public List<ItemRecord> searchItems(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isBlank()) {
            return List.copyOf(items.values());
        }
        return items.values().stream()
            .filter(item -> item.code().toLowerCase().contains(normalized) || item.name().toLowerCase().contains(normalized))
            .toList();
    }

    public ItemRecord findItem(long itemId) {
        ItemRecord item = items.get(itemId);
        if (item == null) {
            throw new IllegalStateException("Item not found");
        }
        return item;
    }

    public ItemRecord findByNameAndSpec(String name, String specification) {
        return items.values().stream()
            .filter(item -> item.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public void addItemImage(long itemId, long attachmentId, boolean primary) {
        ItemRecord item = findItem(itemId);
        item.imageAttachmentIds().add(attachmentId);
        if (primary) {
            item.setPrimaryImageUrl("/api/v1/attachments/" + attachmentId + "/content");
        }
    }

    public List<StockRecord> stockRecords() {
        return List.copyOf(stocks.values());
    }

    public List<StockRecord> searchStocks(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isBlank()) {
            return stockRecords();
        }
        return stocks.values().stream()
            .filter(stock -> matchesStock(stock, normalized))
            .toList();
    }

    public synchronized StockRecord createStock(long warehouseId, long itemId, BigDecimal quantity) {
        assertWarehouseAndItemExist(warehouseId, itemId);
        String key = key(warehouseId, itemId);
        if (stocks.containsKey(key)) {
            throw new IllegalStateException("Stock already exists for warehouse and item");
        }
        StockRecord stock = new StockRecord(stockId.incrementAndGet(), warehouseId, itemId, quantity == null ? BigDecimal.ZERO : quantity);
        stocks.put(key, stock);
        return stock;
    }

    public synchronized void inbound(long oaInstanceId, long actorUserId, long warehouseId, long itemId, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal amount = quantity.multiply(unitPrice);
        addStock(warehouseId, itemId, quantity);
        items.get(itemId).setLatestPrice(unitPrice);
        prices.add(new PriceRecord(priceId.incrementAndGet(), itemId, "purchase", unitPrice, OffsetDateTime.now()));
        transactions.add(new StockTxnRecord(txnId.incrementAndGet(), "INBOUND", "OA-" + oaInstanceId, warehouseId, itemId, quantity, actorUserId, OffsetDateTime.now()));
        inboundOrders.add(order("IN-" + orderId.incrementAndGet(), oaInstanceId, warehouseId, itemId, quantity, unitPrice, amount));
    }

    public synchronized void outbound(long oaInstanceId, long actorUserId, long warehouseId, long itemId, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal current = stocks.getOrDefault(key(warehouseId, itemId), new StockRecord(0, warehouseId, itemId, BigDecimal.ZERO)).quantity();
        if (current.subtract(quantity).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Stock cannot be negative");
        }
        addStock(warehouseId, itemId, quantity.negate());
        BigDecimal amount = quantity.multiply(unitPrice);
        transactions.add(new StockTxnRecord(txnId.incrementAndGet(), "OUTBOUND", "OA-" + oaInstanceId, warehouseId, itemId, quantity.negate(), actorUserId, OffsetDateTime.now()));
        outboundOrders.add(order("OUT-" + orderId.incrementAndGet(), oaInstanceId, warehouseId, itemId, quantity, unitPrice, amount));
    }

    public synchronized void reimburse(long oaInstanceId, long actorUserId, Long relatedInboundOrderId, BigDecimal amount) {
        Map<String, Object> reimbursement = new LinkedHashMap<>();
        reimbursement.put("oaInstanceId", oaInstanceId);
        reimbursement.put("relatedInboundOrderId", relatedInboundOrderId);
        reimbursement.put("applicantUserId", actorUserId);
        reimbursement.put("amount", amount);
        reimbursement.put("status", "approved");
        reimbursements.add(reimbursement);
    }

    private void setStock(long warehouseId, long itemId, BigDecimal quantity) {
        stocks.put(key(warehouseId, itemId), new StockRecord(stockId.incrementAndGet(), warehouseId, itemId, quantity));
    }

    private void addStock(long warehouseId, long itemId, BigDecimal delta) {
        assertWarehouseAndItemExist(warehouseId, itemId);
        String stockKey = key(warehouseId, itemId);
        StockRecord current = stocks.get(stockKey);
        if (current == null) {
            stocks.put(stockKey, new StockRecord(stockId.incrementAndGet(), warehouseId, itemId, delta));
            return;
        }
        stocks.put(stockKey, new StockRecord(current.id(), warehouseId, itemId, current.quantity().add(delta)));
    }

    private String key(long warehouseId, long itemId) {
        return warehouseId + ":" + itemId;
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

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Map<String, Object> order(String orderNo, long oaInstanceId, long warehouseId, long itemId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderNo", orderNo);
        order.put("oaInstanceId", oaInstanceId);
        order.put("warehouseId", warehouseId);
        order.put("itemId", itemId);
        order.put("quantity", quantity);
        order.put("unitPrice", unitPrice);
        order.put("amount", amount);
        order.put("status", "approved");
        return order;
    }
}
