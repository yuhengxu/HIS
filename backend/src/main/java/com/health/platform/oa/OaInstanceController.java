package com.health.platform.oa;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import com.health.platform.inventory.ItemViewRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oa/instances")
public class OaInstanceController {
    private final ProcessRuntimeService processRuntimeService;

    public OaInstanceController(ProcessRuntimeService processRuntimeService) {
        this.processRuntimeService = processRuntimeService;
    }

    @GetMapping("/startable")
    public ApiResponse<List<Map<String, Object>>> startable(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(processRuntimeService.startableProcesses(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping
    public ApiResponse<List<ProcessInstanceRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(processRuntimeService.instances(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/mine")
    public ApiResponse<List<OaInstanceViewRecord>> mine(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(processRuntimeService.myInstances(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/materials/search")
    public ApiResponse<List<ItemViewRecord>> searchMaterials(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam(required = false) String keyword) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        return ApiResponse.ok(processRuntimeService.searchMaterials(actor, keyword).stream().map(item -> processRuntimeService.itemView(actor, item)).toList());
    }

    @GetMapping("/claimable-materials/search")
    public ApiResponse<List<ItemViewRecord>> searchClaimableMaterials(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam long warehouseId, @RequestParam(required = false) String keyword) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        return ApiResponse.ok(processRuntimeService.searchClaimableMaterials(actor, warehouseId, keyword).stream().map(item -> processRuntimeService.itemView(actor, item)).toList());
    }

    @GetMapping("/inbound-materials/search")
    public ApiResponse<List<ItemViewRecord>> searchInboundMaterials(@RequestHeader("X-User-Id") Long actorUserId, @RequestParam long warehouseId, @RequestParam(required = false) String keyword) {
        long actor = SecurityContextUtil.requireUserId(actorUserId);
        return ApiResponse.ok(processRuntimeService.searchInboundMaterials(actor, warehouseId, keyword).stream().map(item -> processRuntimeService.itemView(actor, item)).toList());
    }

    @PostMapping("/inventory-inbound")
    public ApiResponse<ProcessInstanceRecord> startInbound(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(SecurityContextUtil.requireUserId(actorUserId), "inbound_material", "inventory_inbound", "物资入库申请", form));
    }

    @PostMapping("/inventory-outbound")
    public ApiResponse<ProcessInstanceRecord> startOutbound(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(SecurityContextUtil.requireUserId(actorUserId), "outbound_material", "inventory_outbound", "物品领用申请", form));
    }

    @PostMapping("/reimbursement")
    public ApiResponse<ProcessInstanceRecord> startReimbursement(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(SecurityContextUtil.requireUserId(actorUserId), "reimbursement", "reimbursement", "报销申请", form));
    }

    @PostMapping("/{instanceId}/material-drafts")
    public ApiResponse<OaMaterialDraftRecord> createMaterialDraft(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long instanceId, @RequestBody OaMaterialDraftRecord draft) {
        return ApiResponse.ok(processRuntimeService.createMaterialDraft(SecurityContextUtil.requireUserId(actorUserId), instanceId, draft));
    }

    @PostMapping("/{instanceId}/urge")
    public ApiResponse<OaTaskRecord> urge(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long instanceId) {
        return ApiResponse.ok(processRuntimeService.urge(SecurityContextUtil.requireUserId(actorUserId), instanceId));
    }

    @PostMapping("/{instanceId}/revoke")
    public ApiResponse<ProcessInstanceRecord> revoke(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long instanceId) {
        return ApiResponse.ok(processRuntimeService.revoke(SecurityContextUtil.requireUserId(actorUserId), instanceId));
    }
}
