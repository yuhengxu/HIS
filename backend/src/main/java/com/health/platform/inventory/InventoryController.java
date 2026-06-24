package com.health.platform.inventory;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryStockService inventoryStockService;

    public InventoryController(InventoryStockService inventoryStockService) {
        this.inventoryStockService = inventoryStockService;
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseRecord>> warehouses(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.warehouses(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @PostMapping("/warehouses")
    public ApiResponse<WarehouseRecord> createWarehouse(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.WarehouseRequest request) {
        return ApiResponse.ok(inventoryStockService.createWarehouse(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @GetMapping("/items")
    public ApiResponse<List<ItemRecord>> items(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.items(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @PostMapping("/items")
    public ApiResponse<ItemRecord> createItem(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.ItemRequest request) {
        return ApiResponse.ok(inventoryStockService.createItem(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @GetMapping("/prices")
    public ApiResponse<List<PriceRecord>> prices(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.prices(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/stocks")
    public ApiResponse<List<StockRecord>> stocks(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(inventoryStockService.stocks(SecurityContextUtil.requireUserId(actorUserId), keyword));
    }

    @PostMapping("/stocks")
    public ApiResponse<StockRecord> createStock(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.StockRequest request) {
        return ApiResponse.ok(inventoryStockService.createStock(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @GetMapping("/stock-transactions")
    public ApiResponse<List<StockTxnRecord>> transactions(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.transactions(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/inbound-orders")
    public ApiResponse<List<Map<String, Object>>> inboundOrders(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.inboundOrders(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/outbound-orders")
    public ApiResponse<List<Map<String, Object>>> outboundOrders(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.outboundOrders(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/reimbursements")
    public ApiResponse<List<Map<String, Object>>> reimbursements(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.reimbursements(SecurityContextUtil.requireUserId(actorUserId)));
    }
}
