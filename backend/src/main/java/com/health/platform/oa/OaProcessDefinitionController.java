package com.health.platform.oa;

import java.util.List;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oa/process-definitions")
public class OaProcessDefinitionController {
    private final ProcessRuntimeService processRuntimeService;

    public OaProcessDefinitionController(ProcessRuntimeService processRuntimeService) {
        this.processRuntimeService = processRuntimeService;
    }

    @GetMapping
    public ApiResponse<List<ProcessDefinitionRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(processRuntimeService.definitions(SecurityContextUtil.requireUserId(actorUserId)));
    }
}
