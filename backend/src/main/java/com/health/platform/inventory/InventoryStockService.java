package com.health.platform.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import com.health.platform.oa.OaApprovalListener;
import com.health.platform.oa.ProcessInstanceRecord;
import org.springframework.stereotype.Service;

@Service
public class InventoryStockService implements OaApprovalListener {
    private final InventoryStore store;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final IamStore iamStore;

    public InventoryStockService(InventoryStore store, PermissionService permissionService, AuditService auditService, IamStore iamStore) {
        this.store = store;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.iamStore = iamStore;
    }

    private void requireInventoryWriter(long actorUserId) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.require(actorUserId, "inventory:item:write");
    }

    public List<WarehouseRecord> warehouses(long actorUserId) {
        permissionService.require(actorUserId, "inventory:warehouse:read");
        return List.copyOf(store.warehouses());
    }

    public WarehouseRecord createWarehouse(long actorUserId, WarehouseRequest request) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.require(actorUserId, "inventory:warehouse:write");
        return store.addWarehouse(request.code(), request.name(), request.warehouseType());
    }

    public List<ItemRecord> items(long actorUserId) {
        permissionService.require(actorUserId, "inventory:item:read");
        return store.items().stream().filter(item -> canViewItem(actorUserId, item)).toList();
    }

    public List<ItemRecord> searchItems(long actorUserId, String keyword) {
        permissionService.require(actorUserId, "inventory:item:read");
        return store.searchItems(keyword).stream().filter(item -> canViewItem(actorUserId, item)).toList();
    }

    public List<ItemRecord> searchItemsForOa(long actorUserId, String keyword) {
        permissionService.requireAny(actorUserId, "inventory:item:read", "inventory:inbound:create", "inventory:outbound:create");
        return store.searchItems(keyword).stream().filter(item -> canClaimItem(actorUserId, item)).toList();
    }

    public List<ItemRecord> searchClaimableItems(long actorUserId, long warehouseId, String keyword) {
        permissionService.require(actorUserId, "inventory:outbound:create");
        return store.searchClaimableItems(warehouseId, keyword).stream().filter(item -> canClaimItem(actorUserId, item)).toList();
    }

    public List<ItemRecord> searchInboundItems(long actorUserId, long warehouseId, String keyword) {
        permissionService.require(actorUserId, "inventory:inbound:create");
        return store.searchItemsByWarehouseType(warehouseId, keyword).stream().filter(item -> canClaimItem(actorUserId, item)).toList();
    }

    public ItemRecord getItem(long actorUserId, long itemId) {
        permissionService.require(actorUserId, "inventory:item:read");
        try {
            ItemRecord item = store.findItem(itemId);
            requireItemVisible(actorUserId, item);
            return item;
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage());
        }
    }

    public ItemViewRecord toItemView(long actorUserId, ItemRecord item) {
        return new ItemViewRecord(
            item.id(),
            item.code(),
            item.name(),
            item.itemType(),
            item.unit(),
            item.latestPrice(),
            permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN") ? item.materialTag() : null,
            item.primaryImageUrl(),
            List.copyOf(item.imageAttachmentIds()));
    }

    public String itemLabelForOa(long itemId) {
        try {
            ItemRecord item = store.findItem(itemId);
            return item.name() + " (" + item.code() + ")";
        } catch (IllegalStateException ex) {
            return String.valueOf(itemId);
        }
    }

    public String warehouseLabelForOa(long warehouseId) {
        try {
            WarehouseRecord warehouse = store.findWarehouse(warehouseId);
            return warehouse.name();
        } catch (IllegalStateException ex) {
            return String.valueOf(warehouseId);
        }
    }

    public ItemRecord createItem(long actorUserId, ItemRequest request) {
        requireInventoryWriter(actorUserId);
        String tag = normalizeMaterialTag(request.materialTag());
        if ("SPECIAL".equals(tag)) {
            permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        }
        ItemRecord item = store.addItem(request.code(), request.name(), request.itemType(), request.unit(), request.latestPrice(), tag);
        auditService.record(actorUserId, "inventory.item.create", "inv_item", String.valueOf(item.id()));
        return item;
    }

    public ItemRecord updateItem(long actorUserId, long itemId, ItemRequest request) {
        requireInventoryWriter(actorUserId);
        ItemRecord item = getItem(actorUserId, itemId);
        item.update(request.name(), request.itemType(), request.unit(), request.latestPrice());
        if (request.materialTag() != null) {
            permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
            item.setMaterialTag(normalizeMaterialTag(request.materialTag()));
        }
        auditService.record(actorUserId, "inventory.item.update", "inv_item", String.valueOf(item.id()));
        return item;
    }

    public ItemRecord addItemImage(long actorUserId, long itemId, long attachmentId, boolean primary) {
        requireItemImageWriter(actorUserId);
        getItem(actorUserId, itemId);
        long galleryCount = store.listItemImages(itemId).stream().filter(img -> !"main_image".equals(img.usageType())).count();
        if (primary && galleryCount >= 10) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Gallery image limit reached");
        }
        if (!primary && galleryCount >= 10) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Gallery image limit reached");
        }
        store.addItemImage(itemId, attachmentId, primary);
        auditService.record(actorUserId, "inventory.item.image.add", "inv_item", String.valueOf(itemId));
        return store.findItem(itemId);
    }

    public List<ItemImageRecord> listItemImages(long actorUserId, long itemId) {
        permissionService.require(actorUserId, "inventory:item:read");
        getItem(actorUserId, itemId);
        return store.listItemImages(itemId);
    }

    public ItemRecord setMainItemImage(long actorUserId, long itemId, long imageId) {
        requireItemImageWriter(actorUserId);
        try {
            store.setMainItemImage(itemId, imageId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage());
        }
        auditService.record(actorUserId, "inventory.item.image.main", "inv_item", String.valueOf(itemId));
        return store.findItem(itemId);
    }

    public void deleteItemImage(long actorUserId, long itemId, long imageId) {
        requireItemImageWriter(actorUserId);
        try {
            store.deleteItemImage(itemId, imageId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage());
        }
        auditService.record(actorUserId, "inventory.item.image.delete", "inv_item", String.valueOf(itemId));
    }

    private void requireItemImageWriter(long actorUserId) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.requireAny(actorUserId, "inventory:item:image:write", "inventory:image:write");
    }

    private void requireStockImageWriter(long actorUserId) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.require(actorUserId, "inventory:stock:image:write");
    }

    public List<PriceRecord> prices(long actorUserId) {
        permissionService.require(actorUserId, "inventory:price:read");
        return store.prices();
    }

    public List<StockRecord> stocks(long actorUserId) {
        permissionService.require(actorUserId, "inventory:stock:read");
        if (permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN")) {
            return store.stockRecords();
        }
        return store.searchStocks(actorUserId, "");
    }

    public List<StockViewRecord> stockViews(long actorUserId, String keyword) {
        permissionService.require(actorUserId, "inventory:stock:read");
        List<StockRecord> records = permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN")
            ? store.searchStocks(keyword)
            : store.searchStocks(actorUserId, keyword);
        return records.stream().filter(stock -> canViewItem(actorUserId, itemOf(stock.itemId()))).map(this::toStockView).toList();
    }

    public List<StockSummaryViewRecord> stockSummaries(long actorUserId, String keyword) {
        permissionService.require(actorUserId, "inventory:stock:read");
        return store.stockSummaries(keyword).stream().filter(summary -> canViewItem(actorUserId, itemOf(summary.itemId()))).toList();
    }

    public List<StockImageRecord> listStockImages(long actorUserId, long stockId) {
        permissionService.require(actorUserId, "inventory:stock:read");
        try {
            store.findStock(stockId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage());
        }
        return store.listStockImages(stockId);
    }

    public StockImageRecord addStockImage(long actorUserId, long stockId, StockImageRequest request) {
        requireStockImageWriter(actorUserId);
        try {
            StockImageRecord image = store.addStockImage(stockId, request.attachmentId(), request.usageType());
            auditService.record(actorUserId, "inventory.stock.image.add", "inv_stock", String.valueOf(stockId));
            return image;
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    public void deleteStockImage(long actorUserId, long stockId, long imageId) {
        requireStockImageWriter(actorUserId);
        try {
            store.deleteStockImage(stockId, imageId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage());
        }
        auditService.record(actorUserId, "inventory.stock.image.delete", "inv_stock", String.valueOf(stockId));
    }

    public StockRecord createStock(long actorUserId, StockRequest request) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.require(actorUserId, "inventory:stock:write");
        try {
            requireItemVisible(actorUserId, store.findItem(request.itemId()));
            return store.createStock(actorUserId, request.warehouseId(), request.itemId(), request.quantity());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    public List<StockTxnViewRecord> transactions(long actorUserId) {
        permissionService.require(actorUserId, "inventory:stock:read");
        return store.transactions().stream()
            .filter(txn -> permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN") || txn.operatorUserId() == actorUserId)
            .filter(txn -> canViewItem(actorUserId, itemOf(txn.itemId())))
            .map(this::toStockTxnView)
            .toList();
    }

    public StockRecord adjustStock(long actorUserId, StockAdjustRequest request) {
        permissionService.requireRole(actorUserId, "SYSTEM_ADMIN");
        permissionService.require(actorUserId, "inventory:stock:write");
        try {
            StockRecord stock = store.adjustStock(actorUserId, request.warehouseId(), request.itemId(), request.quantity(), request.reason());
            auditService.record(actorUserId, "inventory.stock.adjust", "inv_stock", String.valueOf(stock.id()));
            return stock;
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    public List<Map<String, Object>> inboundOrders(long actorUserId) {
        permissionService.require(actorUserId, "inventory:inbound:approve");
        return store.inboundOrders();
    }

    public List<Map<String, Object>> unlinkedInboundOrdersForReimbursement(long actorUserId) {
        permissionService.require(actorUserId, "finance:reimbursement:create");
        return store.unlinkedInboundOrders(actorUserId);
    }

    public List<Map<String, Object>> outboundOrders(long actorUserId) {
        permissionService.require(actorUserId, "inventory:outbound:approve");
        return store.outboundOrders();
    }

    public List<Map<String, Object>> reimbursements(long actorUserId) {
        permissionService.require(actorUserId, "inventory:price:read");
        return store.reimbursements();
    }

    public ItemRecord createItemFromDraft(String code, String name, String itemType, String unit, BigDecimal latestPrice) {
        if (store.findByNameAndSpec(name, null) != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate item name");
        }
        return store.addItem(code, name, itemType, unit, latestPrice, "NORMAL");
    }

    @Override
    public void onApproved(ProcessInstanceRecord instance) {
        try {
            if ("inventory_inbound".equals(instance.businessType())) {
                long itemId = resolveItemId(instance);
                store.inbound(instance.id(), instance.initiatorUserId(), longValue(instance.formData(), "warehouseId", 1L), itemId, decimalValue(instance.formData(), "quantity", BigDecimal.ONE), decimalValue(instance.formData(), "unitPrice", BigDecimal.ZERO));
            } else if ("inventory_outbound".equals(instance.businessType())) {
                long itemId = resolveItemId(instance);
                store.outbound(instance.id(), instance.initiatorUserId(), longValue(instance.formData(), "warehouseId", 1L), itemId, decimalValue(instance.formData(), "quantity", BigDecimal.ONE), decimalValue(instance.formData(), "unitPrice", BigDecimal.ZERO));
            } else if ("reimbursement".equals(instance.businessType())) {
                store.reimburse(instance.id(), instance.initiatorUserId(), longValue(instance.formData(), "relatedInboundOrderId", null), decimalValue(instance.formData(), "amount", BigDecimal.ZERO));
            }
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    private long resolveItemId(ProcessInstanceRecord instance) {
        Object itemId = instance.formData().get("itemId");
        if (itemId != null) {
            return longValue(instance.formData(), "itemId", 1L);
        }
        Object draftItemId = instance.formData().get("resolvedItemId");
        if (draftItemId != null) {
            return longValue(instance.formData(), "resolvedItemId", 1L);
        }
        Map<String, Object> newMaterial = castMap(instance.formData().get("newMaterial"));
        if (newMaterial != null && !newMaterial.isEmpty()) {
            ItemRecord created = createItemFromDraft(
                String.valueOf(newMaterial.getOrDefault("code", "OA-" + instance.id())),
                String.valueOf(newMaterial.get("name")),
                String.valueOf(newMaterial.getOrDefault("itemType", "non_medical")),
                String.valueOf(newMaterial.get("unit")),
                decimalValue(newMaterial, "defaultPrice", BigDecimal.ZERO));
            instance.formData().put("resolvedItemId", created.id());
            return created.id();
        }
        return longValue(instance.formData(), "itemId", 1L);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private Long longValue(Map<String, Object> form, String key, Long fallback) {
        Object value = form.get(key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal decimalValue(Map<String, Object> form, String key, BigDecimal fallback) {
        Object value = form.get(key);
        if (value == null) return fallback;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    private StockViewRecord toStockView(StockRecord stock) {
        ItemRecord item = null;
        WarehouseRecord warehouse = null;
        try {
            item = store.findItem(stock.itemId());
            warehouse = store.findWarehouse(stock.warehouseId());
        } catch (IllegalStateException ignored) {
            // Read views keep broken references visible with blank labels for auditability.
        }
        return new StockViewRecord(
            stock.id(),
            stock.warehouseId(),
            stock.itemId(),
            stock.quantity(),
            stock.ownerUserId(),
            userName(stock.ownerUserId()),
            item == null ? "" : item.name(),
            item == null ? "" : item.code(),
            warehouse == null ? "" : warehouse.name(),
            warehouse == null ? "" : warehouse.warehouseType(),
            item == null ? null : item.primaryImageUrl(),
            store.listStockImages(stock.id()));
    }

    private StockTxnViewRecord toStockTxnView(StockTxnRecord txn) {
        ItemRecord item = null;
        WarehouseRecord warehouse = null;
        try {
            item = store.findItem(txn.itemId());
            warehouse = store.findWarehouse(txn.warehouseId());
        } catch (IllegalStateException ignored) {
            // Read views keep broken references visible with blank labels for auditability.
        }
        return new StockTxnViewRecord(
            txn.id(),
            txn.txnType(),
            txnTypeName(txn.txnType()),
            txn.bizNo(),
            txn.warehouseId(),
            warehouse == null ? "" : warehouse.name(),
            txn.itemId(),
            item == null ? "" : item.name(),
            item == null ? "" : item.code(),
            txn.quantityDelta(),
            txn.operatorUserId(),
            userName(txn.operatorUserId()),
            txn.occurredAt());
    }

    private String userName(long userId) {
        return iamStore.findUser(userId).map(user -> user.displayName()).orElse(String.valueOf(userId));
    }

    private String txnTypeName(String txnType) {
        if ("INBOUND".equals(txnType)) return "入库";
        if ("OUTBOUND".equals(txnType)) return "出库";
        if ("ADJUST".equals(txnType)) return "库存调整";
        return txnType;
    }

    private ItemRecord itemOf(long itemId) {
        try {
            return store.findItem(itemId);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private boolean canViewItem(long actorUserId, ItemRecord item) {
        if (item == null) return false;
        if (permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN")) return true;
        return !"SPECIAL".equals(item.materialTag());
    }

    private boolean canClaimItem(long actorUserId, ItemRecord item) {
        return canViewItem(actorUserId, item);
    }

    private void requireItemVisible(long actorUserId, ItemRecord item) {
        if (!canViewItem(actorUserId, item)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Current user cannot view special material");
        }
    }

    private String normalizeMaterialTag(String materialTag) {
        if (materialTag == null || materialTag.isBlank()) return "NORMAL";
        return "SPECIAL".equalsIgnoreCase(materialTag) ? "SPECIAL" : "NORMAL";
    }

    public record WarehouseRequest(String code, String name, String warehouseType) {
    }

    public record ItemRequest(String code, String name, String itemType, String unit, BigDecimal latestPrice, String materialTag) {
        public ItemRequest(String code, String name, String itemType, String unit, BigDecimal latestPrice) {
            this(code, name, itemType, unit, latestPrice, null);
        }
    }

    public record StockRequest(long warehouseId, long itemId, BigDecimal quantity) {
    }

    public record StockAdjustRequest(long warehouseId, long itemId, BigDecimal quantity, String reason) {
    }

    public record ItemImageRequest(long attachmentId, boolean primary) {
    }

    public record StockImageRequest(long attachmentId, String usageType) {
    }
}
