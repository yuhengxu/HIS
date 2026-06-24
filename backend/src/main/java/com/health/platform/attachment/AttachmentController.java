package com.health.platform.attachment;

import java.util.List;
import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public ApiResponse<FileAttachmentRecord> upload(@RequestHeader("X-User-Id") Long actorUserId, @RequestBody AttachmentService.UploadRequest request) {
        return ApiResponse.ok(attachmentService.upload(SecurityContextUtil.requireUserId(actorUserId), request));
    }

    @GetMapping("/{attachmentId}")
    public ApiResponse<FileAttachmentRecord> get(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long attachmentId) {
        return ApiResponse.ok(attachmentService.get(SecurityContextUtil.requireUserId(actorUserId), attachmentId));
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> content(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long attachmentId) {
        byte[] content = attachmentService.content(SecurityContextUtil.requireUserId(actorUserId), attachmentId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }

    @PostMapping("/{attachmentId}/bind")
    public ApiResponse<FileAttachmentRecord> bind(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long attachmentId, @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(attachmentService.bind(
            SecurityContextUtil.requireUserId(actorUserId),
            attachmentId,
            String.valueOf(request.get("bizType")),
            Long.parseLong(String.valueOf(request.get("bizId")))));
    }

    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> delete(@RequestHeader("X-User-Id") Long actorUserId, @PathVariable long attachmentId) {
        attachmentService.delete(SecurityContextUtil.requireUserId(actorUserId), attachmentId);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<List<FileAttachmentRecord>> list(@RequestHeader("X-User-Id") Long actorUserId,
                                                          @RequestParam String bizType,
                                                          @RequestParam long bizId) {
        return ApiResponse.ok(attachmentService.listByBiz(SecurityContextUtil.requireUserId(actorUserId), bizType, bizId));
    }
}
