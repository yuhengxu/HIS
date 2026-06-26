package com.health.platform.oa;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import com.health.platform.inventory.ItemViewRecord;
import com.health.platform.wecom.WeComSessionService;
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
    private final WeComSessionService weComSessionService;

    public OaInstanceController(ProcessRuntimeService processRuntimeService, WeComSessionService weComSessionService) {
        this.processRuntimeService = processRuntimeService;
        this.weComSessionService = weComSessionService;
    }

    @GetMapping("/startable")
    public ApiResponse<List<Map<String, Object>>> startable(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(processRuntimeService.startableProcesses(actor(actorUserId, authorization)));
    }

    @GetMapping
    public ApiResponse<List<ProcessInstanceRecord>> list(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(processRuntimeService.instances(actor(actorUserId, authorization)));
    }

    @GetMapping("/mine")
    public ApiResponse<List<OaInstanceViewRecord>> mine(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(processRuntimeService.myInstances(actor(actorUserId, authorization)));
    }

    @GetMapping("/materials/search")
    public ApiResponse<List<ItemViewRecord>> searchMaterials(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam(required = false) String keyword) {
        long actor = actor(actorUserId, authorization);
        return ApiResponse.ok(processRuntimeService.searchMaterials(actor, keyword).stream().map(item -> processRuntimeService.itemView(actor, item)).toList());
    }

    @GetMapping("/claimable-materials/search")
    public ApiResponse<List<ItemViewRecord>> searchClaimableMaterials(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam long warehouseId, @RequestParam(required = false) String keyword) {
        long actor = actor(actorUserId, authorization);
        return ApiResponse.ok(processRuntimeService.searchClaimableMaterials(actor, warehouseId, keyword).stream().map(item -> processRuntimeService.itemView(actor, item)).toList());
    }

    @GetMapping("/inbound-materials/search")
    public ApiResponse<List<ItemViewRecord>> searchInboundMaterials(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam long warehouseId, @RequestParam(required = false) String keyword) {
        long actor = actor(actorUserId, authorization);
        return ApiResponse.ok(processRuntimeService.searchInboundMaterials(actor, warehouseId, keyword).stream().map(item -> processRuntimeService.itemView(actor, item)).toList());
    }

    @PostMapping("/inventory-inbound")
    public ApiResponse<ProcessInstanceRecord> startInbound(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(actor(actorUserId, authorization), "inbound_material", "inventory_inbound", "物资入库申请", form));
    }

    @PostMapping("/inventory-outbound")
    public ApiResponse<ProcessInstanceRecord> startOutbound(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(actor(actorUserId, authorization), "outbound_material", "inventory_outbound", "物品领用申请", form));
    }

    @PostMapping("/reimbursement")
    public ApiResponse<ProcessInstanceRecord> startReimbursement(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(actor(actorUserId, authorization), "reimbursement", "reimbursement", "报销申请", form));
    }

    @PostMapping("/{instanceId}/material-drafts")
    public ApiResponse<OaMaterialDraftRecord> createMaterialDraft(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long instanceId, @RequestBody OaMaterialDraftRecord draft) {
        return ApiResponse.ok(processRuntimeService.createMaterialDraft(actor(actorUserId, authorization), instanceId, draft));
    }

    @PostMapping("/{instanceId}/urge")
    public ApiResponse<OaTaskRecord> urge(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long instanceId) {
        return ApiResponse.ok(processRuntimeService.urge(actor(actorUserId, authorization), instanceId));
    }

    @PostMapping("/{instanceId}/revoke")
    public ApiResponse<ProcessInstanceRecord> revoke(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long instanceId) {
        return ApiResponse.ok(processRuntimeService.revoke(actor(actorUserId, authorization), instanceId));
    }

    private long actor(Long actorUserId, String authorization) {
        return SecurityContextUtil.requireUserId(actorUserId, authorization, weComSessionService::requireUserId);
    }
}
