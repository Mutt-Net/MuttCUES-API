package net.muttcode.spring.service;

import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.repository.ProcessingJobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProcessingJobService {

    private final ProcessingJobRepository repository;

    public ProcessingJobService(ProcessingJobRepository repository) {
        this.repository = repository;
    }

    public ProcessingJob saveJob(ProcessingJob job) {
        return repository.save(job);
    }

    public Optional<ProcessingJob> getJob(String jobId) {
        return repository.findById(jobId);
    }

    public List<ProcessingJob> getAllJobs() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<ProcessingJob> getJobsByStatus(ProcessingJob.JobStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    public void updateJobProgress(String jobId, Integer progressPercent) {
        repository.findById(jobId).ifPresent(job -> {
            job.setProgressPercent(progressPercent);
            repository.save(job);
        });
    }

    public void startProcessing(String jobId) {
        repository.findById(jobId).ifPresent(job -> {
            job.setStatus(ProcessingJob.JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            job.setProgressPercent(0);
            repository.save(job);
        });
    }

    public void completeJob(String jobId, String outputFileId, Long processingTimeMs) {
        repository.findById(jobId).ifPresent(job -> {
            job.setStatus(ProcessingJob.JobStatus.COMPLETED);
            job.setOutputFileId(outputFileId);
            job.setProgressPercent(100);
            job.setCompletedAt(Instant.now());
            job.setProcessingTimeMs(processingTimeMs);
            repository.save(job);
        });
    }

    public void failJob(String jobId, String errorMessage) {
        repository.findById(jobId).ifPresent(job -> {
            job.setStatus(ProcessingJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(Instant.now());
            repository.save(job);
        });
    }
}
