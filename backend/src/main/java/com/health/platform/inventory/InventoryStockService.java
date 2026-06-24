package com.health.platform.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.PermissionService;
import com.health.platform.oa.OaApprovalListener;
import com.health.platform.oa.ProcessInstanceRecord;
import org.springframework.stereotype.Service;

@Service
public class InventoryStockService implements OaApprovalListener {
    private final InventoryStore store;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public InventoryStockService(InventoryStore store, PermissionService permissionService, AuditService auditService) {
        this.store = store;
        this.permissionService = permissionService;
        this.auditService = auditService;
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
        return List.copyOf(store.items());
    }

    public List<ItemRecord> searchItems(long actorUserId, String keyword) {
        permissionService.require(actorUserId, "inventory:item:read");
        return store.searchItems(keyword);
    }

    public ItemRecord getItem(long actorUserId, long itemId) {
        permissionService.require(actorUserId, "inventory:item:read");
        try {
            return store.findItem(itemId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ex.getMessage());
        }
    }

    public ItemRecord createItem(long actorUserId, ItemRequest request) {
        requireInventoryWriter(actorUserId);
        ItemRecord item = store.addItem(request.code(), request.name(), request.itemType(), request.unit(), request.latestPrice());
        auditService.record(actorUserId, "inventory.item.create", "inv_item", String.valueOf(item.id()));
        return item;
    }

    public ItemRecord updateItem(long actorUserId, long itemId, ItemRequest request) {
        requireInventoryWriter(actorUserId);
        ItemRecord item = getItem(actorUserId, itemId);
        item.update(request.name(), request.itemType(), request.unit(), request.latestPrice());
        auditService.record(actorUserId, "inventory.item.update", "inv_item", String.valueOf(item.id()));
        return item;
    }

    public ItemRecord addItemImage(long actorUserId, long itemId, long attachmentId, boolean primary) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.require(actorUserId, "inventory:image:write");
        getItem(actorUserId, itemId);
        store.addItemImage(itemId, attachmentId, primary);
        auditService.record(actorUserId, "inventory.item.image.add", "inv_item", String.valueOf(itemId));
        return store.findItem(itemId);
    }

    public List<PriceRecord> prices(long actorUserId) {
        permissionService.require(actorUserId, "inventory:price:read");
        return store.prices();
    }

    public List<StockRecord> stocks(long actorUserId) {
        permissionService.require(actorUserId, "inventory:stock:read");
        return store.stockRecords();
    }

    public List<StockRecord> stocks(long actorUserId, String keyword) {
        permissionService.require(actorUserId, "inventory:stock:read");
        return store.searchStocks(keyword);
    }

    public StockRecord createStock(long actorUserId, StockRequest request) {
        permissionService.requireAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN");
        permissionService.require(actorUserId, "inventory:stock:write");
        try {
            return store.createStock(request.warehouseId(), request.itemId(), request.quantity());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
    }

    public List<StockTxnRecord> transactions(long actorUserId) {
        permissionService.require(actorUserId, "inventory:stock:read");
        return store.transactions();
    }

    public List<Map<String, Object>> inboundOrders(long actorUserId) {
        permissionService.require(actorUserId, "inventory:inbound:approve");
        return store.inboundOrders();
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
        return store.addItem(code, name, itemType, unit, latestPrice);
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

    public record WarehouseRequest(String code, String name, String warehouseType) {
    }

    public record ItemRequest(String code, String name, String itemType, String unit, BigDecimal latestPrice) {
    }

    public record StockRequest(long warehouseId, long itemId, BigDecimal quantity) {
    }

    public record ItemImageRequest(long attachmentId, boolean primary) {
    }
}
