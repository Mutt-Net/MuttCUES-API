package net.muttcode.spring.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_files")
public class ProcessedFile {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "processed_file_id", nullable = false, unique = true, length = 255)
    private String processedFileId;

    @Column(name = "processed_name", nullable = false, length = 500)
    private String processedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_type", nullable = false, length = 50)
    private ProcessingType processingType;

    @Column(name = "processing_params", columnDefinition = "TEXT")
    private String processingParams;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProcessedFileStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processing_completed_at")
    private Instant processingCompletedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ProcessingType {
        UPSCALE, DDS_TO_PNG, IMAGE_TO_DDS
    }

    public enum ProcessedFileStatus {
        PROCESSING, COMPLETED, FAILED
    }

    public ProcessedFile() {}

    public ProcessedFile(File file, String processedFileId, String processedName, ProcessingType processingType) {
        this.id = UUID.randomUUID().toString();
        this.file = file;
        this.processedFileId = processedFileId;
        this.processedName = processedName;
        this.processingType = processingType;
        this.status = ProcessedFileStatus.PROCESSING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }

    public String getProcessedFileId() { return processedFileId; }
    public void setProcessedFileId(String processedFileId) { this.processedFileId = processedFileId; }

    public String getProcessedName() { return processedName; }
    public void setProcessedName(String processedName) { this.processedName = processedName; }

    public ProcessingType getProcessingType() { return processingType; }
    public void setProcessingType(ProcessingType processingType) { this.processingType = processingType; }

    public String getProcessingParams() { return processingParams; }
    public void setProcessingParams(String processingParams) { this.processingParams = processingParams; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public ProcessedFileStatus getStatus() { return status; }
    public void setStatus(ProcessedFileStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getProcessingStartedAt() { return processingStartedAt; }
    public void setProcessingStartedAt(Instant processingStartedAt) { this.processingStartedAt = processingStartedAt; }

    public Instant getProcessingCompletedAt() { return processingCompletedAt; }
    public void setProcessingCompletedAt(Instant processingCompletedAt) { this.processingCompletedAt = processingCompletedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
