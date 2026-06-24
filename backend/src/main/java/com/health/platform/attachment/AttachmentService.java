package com.health.platform.attachment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.health.platform.audit.AuditService;
import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class AttachmentService {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> VOUCHER_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    private static final long VOUCHER_MAX_BYTES = 10L * 1024 * 1024;

    private final AttachmentStore store;
    private final PermissionService permissionService;
    private final IamStore iamStore;
    private final AuditService auditService;

    public AttachmentService(AttachmentStore store, PermissionService permissionService, IamStore iamStore, AuditService auditService) {
        this.store = store;
        this.permissionService = permissionService;
        this.iamStore = iamStore;
        this.auditService = auditService;
    }

    public FileAttachmentRecord upload(long actorUserId, UploadRequest request) {
        validateUploadPermission(actorUserId, request.usageType());
        validateFile(request);
        byte[] content = decodeContent(request.contentBase64());
        if (content.length > maxBytes(request.usageType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File exceeds size limit");
        }
        String checksum = Integer.toHexString(java.util.Arrays.hashCode(content));
        String storagePath = "uploads/" + request.usageType() + "/" + System.currentTimeMillis() + "-" + request.originalName();
        FileAttachmentRecord attachment = store.create(
            request.bizType(), request.bizId(), request.usageType(), request.originalName(),
            storagePath, request.contentType(), content.length, checksum, actorUserId);
        auditService.record(actorUserId, "attachment.upload", "file_attachment", String.valueOf(attachment.id()));
        return attachment;
    }

    public FileAttachmentRecord get(long actorUserId, long attachmentId) {
        FileAttachmentRecord attachment = store.find(attachmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Attachment not found"));
        validateReadPermission(actorUserId, attachment);
        return attachment;
    }

    public byte[] content(long actorUserId, long attachmentId) {
        get(actorUserId, attachmentId);
        return ("placeholder-content-" + attachmentId).getBytes(StandardCharsets.UTF_8);
    }

    public FileAttachmentRecord bind(long actorUserId, long attachmentId, String bizType, long bizId) {
        permissionService.requireAny(actorUserId, "oa:attachment:write", "inventory:image:write");
        FileAttachmentRecord attachment = store.find(attachmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Attachment not found"));
        if (attachment.uploadedBy() != actorUserId && !permissionService.hasAnyRole(actorUserId, "SYSTEM_ADMIN", "INVENTORY_ADMIN", "OA_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot bind attachment uploaded by others");
        }
        attachment.bindBiz(bizType, bizId);
        auditService.record(actorUserId, "attachment.bind", "file_attachment", String.valueOf(attachmentId));
        return attachment;
    }

    public void delete(long actorUserId, long attachmentId) {
        FileAttachmentRecord attachment = store.find(attachmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Attachment not found"));
        if (attachment.uploadedBy() != actorUserId && !permissionService.hasRole(actorUserId, "SYSTEM_ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot delete attachment");
        }
        attachment.softDelete();
        auditService.record(actorUserId, "attachment.delete", "file_attachment", String.valueOf(attachmentId));
    }

    public List<FileAttachmentRecord> listByBiz(long actorUserId, String bizType, long bizId) {
        permissionService.requireAny(actorUserId, "inventory:item:read", "inventory:stock:read", "oa:instance:read", "oa:attachment:write");
        return store.all().stream()
            .filter(a -> bizType.equals(a.bizType()) && bizId == (a.bizId() == null ? -1 : a.bizId()))
            .toList();
    }

    private void validateUploadPermission(long actorUserId, String usageType) {
        if ("material_image".equals(usageType)) {
            permissionService.requireAny(actorUserId, "inventory:image:write", "oa:attachment:write");
            return;
        }
        if ("reimbursement_voucher".equals(usageType) || "oa_attachment".equals(usageType)) {
            permissionService.require(actorUserId, "oa:attachment:write");
            return;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported usage type");
    }

    private void validateReadPermission(long actorUserId, FileAttachmentRecord attachment) {
        if (attachment.uploadedBy() == actorUserId) {
            return;
        }
        if ("material_image".equals(attachment.usageType())) {
            permissionService.requireAny(actorUserId, "inventory:item:read", "inventory:stock:read", "inventory:image:write");
            return;
        }
        permissionService.requireAny(actorUserId, "oa:instance:read", "oa:attachment:write", "finance:reimbursement:approve");
    }

    private void validateFile(UploadRequest request) {
        String ext = extension(request.originalName());
        if ("material_image".equals(request.usageType()) && !IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid image file type");
        }
        if ("reimbursement_voucher".equals(request.usageType()) && !VOUCHER_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid voucher file type");
        }
        if ("oa_attachment".equals(request.usageType()) && !IMAGE_EXTENSIONS.contains(ext) && !VOUCHER_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid attachment file type");
        }
    }

    private long maxBytes(String usageType) {
        return "reimbursement_voucher".equals(usageType) ? VOUCHER_MAX_BYTES : IMAGE_MAX_BYTES;
    }

    private byte[] decodeContent(String contentBase64) {
        if (contentBase64 == null || contentBase64.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File content is required");
        }
        String payload = contentBase64.contains(",") ? contentBase64.substring(contentBase64.indexOf(',') + 1) : contentBase64;
        return Base64.getDecoder().decode(payload);
    }

    private String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record UploadRequest(String bizType, Long bizId, String usageType, String originalName, String contentType, String contentBase64) {
    }
}
