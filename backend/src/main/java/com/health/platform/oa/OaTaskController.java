package com.health.platform.oa;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import com.health.platform.wecom.WeComSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oa/tasks")
public class OaTaskController {
    private final ProcessRuntimeService processRuntimeService;
    private final WeComSessionService weComSessionService;

    public OaTaskController(ProcessRuntimeService processRuntimeService, WeComSessionService weComSessionService) {
        this.processRuntimeService = processRuntimeService;
        this.weComSessionService = weComSessionService;
    }

    @GetMapping("/todo")
    public ApiResponse<List<OaTaskRecord>> todo(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(processRuntimeService.todo(actor(actorUserId, authorization)));
    }

    @GetMapping("/handled")
    public ApiResponse<List<OaTaskRecord>> handled(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(processRuntimeService.handled(actor(actorUserId, authorization)));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<OaTaskDetailRecord> detail(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long taskId) {
        return ApiResponse.ok(processRuntimeService.taskDetail(actor(actorUserId, authorization), taskId));
    }

    @PostMapping("/{taskId}/approve")
    public ApiResponse<ProcessInstanceRecord> approve(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long taskId, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(processRuntimeService.approve(actor(actorUserId, authorization), taskId, body == null ? null : body.get("comment")));
    }

    @PostMapping("/{taskId}/reject")
    public ApiResponse<ProcessInstanceRecord> reject(@RequestHeader(value = "X-User-Id", required = false) Long actorUserId, @RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable long taskId, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(processRuntimeService.reject(actor(actorUserId, authorization), taskId, body == null ? null : body.get("comment")));
    }

    private long actor(Long actorUserId, String authorization) {
        return SecurityContextUtil.requireUserId(actorUserId, authorization, weComSessionService::requireUserId);
    }
}
