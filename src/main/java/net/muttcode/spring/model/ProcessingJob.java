package net.muttcode.spring.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    @Column(name = "job_id", length = 36)
    private String jobId;

    @Column(name = "input_file_id", nullable = false)
    private String inputFileId;

    @Column(name = "output_file_id")
    private String outputFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "scale_factor")
    private Integer scaleFactor;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    public enum JobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED
    }

    public ProcessingJob() {}

    public ProcessingJob(String jobId, String inputFileId, Integer scaleFactor, String modelName) {
        this.jobId = jobId;
        this.inputFileId = inputFileId;
        this.scaleFactor = scaleFactor;
        this.modelName = modelName;
        this.status = JobStatus.QUEUED;
        this.progressPercent = 0;
        this.createdAt = Instant.now();
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getInputFileId() { return inputFileId; }
    public void setInputFileId(String inputFileId) { this.inputFileId = inputFileId; }
    public String getOutputFileId() { return outputFileId; }
    public void setOutputFileId(String outputFileId) { this.outputFileId = outputFileId; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public Integer getScaleFactor() { return scaleFactor; }
    public void setScaleFactor(Integer scaleFactor) { this.scaleFactor = scaleFactor; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
