package com.health.platform.oa;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oa/process-definitions")
public class OaProcessDefinitionController {
    private final OaProcessDefinitionService definitionService;

    public OaProcessDefinitionController(OaProcessDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @GetMapping
    public ApiResponse<List<ProcessDefinitionRecord>> list(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(definitionService.list(SecurityContextUtil.requireUserId(actorUserId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProcessDefinitionRecord> get(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id) {
        ProcessDefinitionRecord definition = definitionService.get(SecurityContextUtil.requireUserId(actorUserId), id);
        return ApiResponse.ok(definition);
    }

    @PostMapping
    public ApiResponse<ProcessDefinitionRecord> create(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody OaProcessDefinitionService.DefinitionRequest request) {
        return ApiResponse.ok(definitionService.create(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProcessDefinitionRecord> update(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id, @RequestBody OaProcessDefinitionService.DefinitionRequest request) {
        return ApiResponse.ok(definitionService.update(SecurityContextUtil.requireUserId(actorUserId), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id) {
        definitionService.delete(SecurityContextUtil.requireUserId(actorUserId), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ProcessDefinitionRecord> publish(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id) {
        return ApiResponse.ok(definitionService.publish(SecurityContextUtil.requireUserId(actorUserId), id));
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<ProcessDefinitionRecord> copy(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id, @RequestBody OaProcessDefinitionService.CopyRequest request) {
        return ApiResponse.ok(definitionService.copy(SecurityContextUtil.requireUserId(actorUserId), id, request));
    }

    @GetMapping("/{id}/nodes")
    public ApiResponse<List<ProcessNodeRecord>> nodes(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id) {
        ProcessDefinitionRecord definition = definitionService.get(SecurityContextUtil.requireUserId(actorUserId), id);
        return ApiResponse.ok(definition.nodes());
    }

    @PutMapping("/{id}/nodes")
    public ApiResponse<ProcessDefinitionRecord> saveNodes(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id, @RequestBody List<ProcessNodeRecord.NodeRequest> nodes) {
        return ApiResponse.ok(definitionService.saveNodes(SecurityContextUtil.requireUserId(actorUserId), id, nodes));
    }

    @PostMapping("/{id}/nodes/validate")
    public ApiResponse<Map<String, String>> validateNodes(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long id, @RequestBody List<ProcessNodeRecord.NodeRequest> nodes) {
        definitionService.validateNodes(SecurityContextUtil.requireUserId(actorUserId), id, nodes);
        return ApiResponse.ok(Map.of("valid", "true"));
    }
}
