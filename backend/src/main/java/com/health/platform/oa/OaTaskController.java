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
@RequestMapping("/api/v1/oa/tasks")
public class OaTaskController {
    private final ProcessRuntimeService processRuntimeService;

    public OaTaskController(ProcessRuntimeService processRuntimeService) {
        this.processRuntimeService = processRuntimeService;
    }

    @GetMapping("/todo")
    public ApiResponse<List<OaTaskRecord>> todo(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(processRuntimeService.todo(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @PostMapping("/{taskId}/approve")
    public ApiResponse<ProcessInstanceRecord> approve(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long taskId, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(processRuntimeService.approve(SecurityContextUtil.requireUserId(actorUserId), taskId, body == null ? null : body.get("comment")));
    }

    @PostMapping("/{taskId}/reject")
    public ApiResponse<ProcessInstanceRecord> reject(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long taskId, @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(processRuntimeService.reject(SecurityContextUtil.requireUserId(actorUserId), taskId, body == null ? null : body.get("comment")));
    }
}
