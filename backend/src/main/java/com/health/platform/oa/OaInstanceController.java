package com.health.platform.oa;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oa/instances")
public class OaInstanceController {
    private final ProcessRuntimeService processRuntimeService;

    public OaInstanceController(ProcessRuntimeService processRuntimeService) {
        this.processRuntimeService = processRuntimeService;
    }

    @GetMapping
    public ApiResponse<List<ProcessInstanceRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(processRuntimeService.instances(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @PostMapping("/inventory-inbound")
    public ApiResponse<ProcessInstanceRecord> startInbound(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(SecurityContextUtil.requireUserId(actorUserId), "inbound_material", "inventory_inbound", "物资入库申请", form));
    }

    @PostMapping("/inventory-outbound")
    public ApiResponse<ProcessInstanceRecord> startOutbound(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(SecurityContextUtil.requireUserId(actorUserId), "outbound_material", "inventory_outbound", "物资出库申请", form));
    }

    @PostMapping("/reimbursement")
    public ApiResponse<ProcessInstanceRecord> startReimbursement(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody Map<String, Object> form) {
        return ApiResponse.ok(processRuntimeService.start(SecurityContextUtil.requireUserId(actorUserId), "reimbursement", "reimbursement", "报销申请", form));
    }

    @PostMapping("/{instanceId}/urge")
    public ApiResponse<OaTaskRecord> urge(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long instanceId) {
        return ApiResponse.ok(processRuntimeService.urge(SecurityContextUtil.requireUserId(actorUserId), instanceId));
    }
}
