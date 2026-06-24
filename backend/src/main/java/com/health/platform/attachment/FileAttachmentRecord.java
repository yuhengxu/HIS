package com.health.platform.attachment;

import java.time.OffsetDateTime;

public class FileAttachmentRecord {
    private final long id;
    private String bizType;
    private Long bizId;
    private String usageType;
    private String originalName;
    private String storagePath;
    private String contentType;
    private long sizeBytes;
    private String checksum;
    private long uploadedBy;
    private final OffsetDateTime createdAt;
    private OffsetDateTime deletedAt;

    public FileAttachmentRecord(long id, String bizType, Long bizId, String usageType, String originalName,
                                String storagePath, String contentType, long sizeBytes, String checksum, long uploadedBy) {
        this.id = id;
        this.bizType = bizType;
        this.bizId = bizId;
        this.usageType = usageType;
        this.originalName = originalName;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.uploadedBy = uploadedBy;
        this.createdAt = OffsetDateTime.now();
    }

    public long id() { return id; }
    public String bizType() { return bizType; }
    public Long bizId() { return bizId; }
    public String usageType() { return usageType; }
    public String originalName() { return originalName; }
    public String storagePath() { return storagePath; }
    public String contentType() { return contentType; }
    public long sizeBytes() { return sizeBytes; }
    public String checksum() { return checksum; }
    public long uploadedBy() { return uploadedBy; }
    public OffsetDateTime createdAt() { return createdAt; }
    public OffsetDateTime deletedAt() { return deletedAt; }

    public void bindBiz(String bizType, long bizId) {
        this.bizType = bizType;
        this.bizId = bizId;
    }

    public void softDelete() { this.deletedAt = OffsetDateTime.now(); }

    public long getId() { return id(); }
    public String getBizType() { return bizType(); }
    public Long getBizId() { return bizId(); }
    public String getUsageType() { return usageType(); }
    public String getOriginalName() { return originalName(); }
    public String getStoragePath() { return storagePath(); }
    public String getContentType() { return contentType(); }
    public long getSizeBytes() { return sizeBytes(); }
    public String getChecksum() { return checksum(); }
    public long getUploadedBy() { return uploadedBy(); }
    public OffsetDateTime getCreatedAt() { return createdAt(); }
    public String getUrl() { return "/api/v1/attachments/" + id + "/content"; }
}
