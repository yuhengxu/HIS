package com.health.platform.attachment;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class AttachmentStore {
    private final AtomicLong idSeq = new AtomicLong(0);
    private final Map<Long, FileAttachmentRecord> attachments = new LinkedHashMap<>();

    public FileAttachmentRecord save(FileAttachmentRecord attachment) {
        attachments.put(attachment.id(), attachment);
        return attachment;
    }

    public FileAttachmentRecord create(String bizType, Long bizId, String usageType, String originalName,
                                       String storagePath, String contentType, long sizeBytes, String checksum, long uploadedBy) {
        FileAttachmentRecord attachment = new FileAttachmentRecord(
            idSeq.incrementAndGet(), bizType, bizId, usageType, originalName, storagePath, contentType, sizeBytes, checksum, uploadedBy);
        attachments.put(attachment.id(), attachment);
        return attachment;
    }

    public Optional<FileAttachmentRecord> find(long id) {
        return Optional.ofNullable(attachments.get(id)).filter(a -> a.deletedAt() == null);
    }

    public Collection<FileAttachmentRecord> all() {
        return attachments.values().stream().filter(a -> a.deletedAt() == null).toList();
    }
}
