package com.health.platform.inventory;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import com.health.platform.wecom.WeComSessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryStockService inventoryStockService;
    private final WeComSessionService weComSessionService;

    public InventoryController(InventoryStockService inventoryStockService, WeComSessionService weComSessionService) {
        this.inventoryStockService = inventoryStockService;
        this.weComSessionService = weComSessionService;
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseRecord>> warehouses(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(inventoryStockService.warehouses(actor(actorUserId, authorization)));
    }

    @PostMapping("/warehouses")
    public ApiResponse<WarehouseRecord> createWarehouse(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.WarehouseRequest request) {
        return ApiResponse.ok(inventoryStockService.createWarehouse(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @GetMapping("/items")
    public ApiResponse<List<ItemViewRecord>> items(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam(required = false) String keyword) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        if (keyword != null && !keyword.isBlank()) {
            return ApiResponse.ok(inventoryStockService.searchItems(actor, keyword).stream().map(item -> inventoryStockService.toItemView(actor, item)).toList());
        }
        return ApiResponse.ok(inventoryStockService.items(actor).stream().map(item -> inventoryStockService.toItemView(actor, item)).toList());
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<ItemViewRecord> getItem(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        return ApiResponse.ok(inventoryStockService.toItemView(actor, inventoryStockService.getItem(actor, itemId)));
    }

    @PostMapping("/items")
    public ApiResponse<ItemViewRecord> createItem(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.ItemRequest request) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        return ApiResponse.ok(inventoryStockService.toItemView(actor, inventoryStockService.createItem(actor, request)));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<ItemViewRecord> updateItem(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId, @RequestBody InventoryStockService.ItemRequest request) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        return ApiResponse.ok(inventoryStockService.toItemView(actor, inventoryStockService.updateItem(actor, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId) {
        inventoryStockService.deleteItem(SecurityContextUtil.requireUserId(actorUserId), itemId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/items/{itemId}/images")
    public ApiResponse<List<ItemImageRecord>> listItemImages(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId) {
        return ApiResponse.ok(inventoryStockService.listItemImages(SecurityContextUtil.requireUserId(actorUserId), itemId));
    }

    @PostMapping("/items/{itemId}/images")
    public ApiResponse<ItemRecord> addItemImage(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId, @RequestBody InventoryStockService.ItemImageRequest request) {
        return ApiResponse.ok(inventoryStockService.addItemImage(SecurityContextUtil.requireUserId(actorUserId), itemId, request.attachmentId(), request.primary()));
    }

    @PutMapping("/items/{itemId}/images/{imageId}/main")
    public ApiResponse<ItemRecord> setMainItemImage(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId, @PathVariable long imageId) {
        return ApiResponse.ok(inventoryStockService.setMainItemImage(SecurityContextUtil.requireUserId(actorUserId), itemId, imageId));
    }

    @DeleteMapping("/items/{itemId}/images/{imageId}")
    public ApiResponse<Void> deleteItemImage(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long itemId, @PathVariable long imageId) {
        inventoryStockService.deleteItemImage(SecurityContextUtil.requireUserId(actorUserId), itemId, imageId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/prices")
    public ApiResponse<List<PriceRecord>> prices(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.prices(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/stocks")
    public ApiResponse<List<StockViewRecord>> stocks(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(inventoryStockService.stockViews(SecurityContextUtil.requireUserId(actorUserId), keyword));
    }

    @GetMapping("/stock-summary")
    public ApiResponse<List<StockSummaryViewRecord>> stockSummary(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(inventoryStockService.stockSummaries(SecurityContextUtil.requireUserId(actorUserId), keyword));
    }

    @PostMapping("/stocks")
    public ApiResponse<StockRecord> createStock(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.StockRequest request) {
        return ApiResponse.ok(inventoryStockService.createStock(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @PostMapping("/stocks/adjust")
    public ApiResponse<StockRecord> adjustStock(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody InventoryStockService.StockAdjustRequest request) {
        return ApiResponse.ok(inventoryStockService.adjustStock(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @GetMapping("/stocks/{stockId}/images")
    public ApiResponse<List<StockImageRecord>> listStockImages(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long stockId) {
        return ApiResponse.ok(inventoryStockService.listStockImages(SecurityContextUtil.requireUserId(actorUserId), stockId));
    }

    @PostMapping("/stocks/{stockId}/images")
    public ApiResponse<StockImageRecord> addStockImage(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long stockId, @RequestBody InventoryStockService.StockImageRequest request) {
        return ApiResponse.ok(inventoryStockService.addStockImage(SecurityContextUtil.requireUserId(actorUserId), stockId, request));
    }

    @DeleteMapping("/stocks/{stockId}/images/{imageId}")
    public ApiResponse<Void> deleteStockImage(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long stockId, @PathVariable long imageId) {
        inventoryStockService.deleteStockImage(SecurityContextUtil.requireUserId(actorUserId), stockId, imageId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stock-transactions")
    public ApiResponse<List<StockTxnViewRecord>> transactions(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.transactions(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/inbound-orders")
    public ApiResponse<List<Map<String, Object>>> inboundOrders(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(inventoryStockService.inboundOrders(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/inbound-orders/unlinked-for-reimbursement")
    public ApiResponse<List<Map<String, Object>>> unlinkedInboundOrdersForReimbursement(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(inventoryStockService.unlinkedInboundOrdersForReimbursement(actor(actorUserId, authorization)));
    }

    private long actor(Long actorUserId, String authorization) {
        return SecurityContextUtil.requireUserId(actorUserId, authorization, weComSessionService::requireUserId);
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
