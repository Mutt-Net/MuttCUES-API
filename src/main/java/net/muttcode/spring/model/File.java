package net.muttcode.spring.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "files")
public class File {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "file_id", nullable = false, unique = true, length = 255)
    private String fileId;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 500)
    private String storedName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate;

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FileStatus status;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum FileStatus {
        UPLOADED, PROCESSING, COMPLETED, FAILED, DELETED
    }

    public File() {}

    public File(String fileId, String originalName, String storedName, Long fileSize, String contentType) {
        this.id = java.util.UUID.randomUUID().toString();
        this.fileId = fileId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadDate = Instant.now();
        this.status = FileStatus.UPLOADED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Instant getUploadDate() { return uploadDate; }
    public void setUploadDate(Instant uploadDate) { this.uploadDate = uploadDate; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}