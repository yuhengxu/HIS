package com.health.platform.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    public InventoryStockService(InventoryStore store, PermissionService permissionService) {
        this.store = store;
        this.permissionService = permissionService;
    }

    public List<WarehouseRecord> warehouses(long actorUserId) {
        permissionService.require(actorUserId, "inventory:warehouse:read");
        return List.copyOf(store.warehouses());
    }

    public WarehouseRecord createWarehouse(long actorUserId, WarehouseRequest request) {
        permissionService.require(actorUserId, "inventory:warehouse:write");
        return store.addWarehouse(request.code(), request.name(), request.warehouseType());
    }

    public List<ItemRecord> items(long actorUserId) {
        permissionService.require(actorUserId, "inventory:item:read");
        return List.copyOf(store.items());
    }

    public ItemRecord createItem(long actorUserId, ItemRequest request) {
        permissionService.require(actorUserId, "inventory:item:write");
        return store.addItem(request.code(), request.name(), request.itemType(), request.unit(), request.latestPrice());
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

    @Override
    public void onApproved(ProcessInstanceRecord instance) {
        try {
            if ("inventory_inbound".equals(instance.businessType())) {
                store.inbound(instance.id(), instance.initiatorUserId(), longValue(instance.formData(), "warehouseId", 1L), longValue(instance.formData(), "itemId", 1L), decimalValue(instance.formData(), "quantity", BigDecimal.ONE), decimalValue(instance.formData(), "unitPrice", BigDecimal.ZERO));
            } else if ("inventory_outbound".equals(instance.businessType())) {
                store.outbound(instance.id(), instance.initiatorUserId(), longValue(instance.formData(), "warehouseId", 1L), longValue(instance.formData(), "itemId", 1L), decimalValue(instance.formData(), "quantity", BigDecimal.ONE), decimalValue(instance.formData(), "unitPrice", BigDecimal.ZERO));
            } else if ("reimbursement".equals(instance.businessType())) {
                store.reimburse(instance.id(), instance.initiatorUserId(), longValue(instance.formData(), "relatedInboundOrderId", null), decimalValue(instance.formData(), "amount", BigDecimal.ZERO));
            }
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ex.getMessage());
        }
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
}
