package net.muttcode.spring.repository;

import net.muttcode.spring.model.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, String> {
    List<ProcessingJob> findByStatusOrderByCreatedAtDesc(ProcessingJob.JobStatus status);
    List<ProcessingJob> findAllByOrderByCreatedAtDesc();

    // Dashboard queries
    List<ProcessingJob> findByStatusAndCreatedAtBefore(ProcessingJob.JobStatus status, Instant before);
    long countByStatusAndCreatedAtAfter(ProcessingJob.JobStatus status, Instant since);
    List<ProcessingJob> findTop10ByStatusOrderByCompletedAtDesc(ProcessingJob.JobStatus status);
    List<ProcessingJob> findTop5ByStatusOrderByCompletedAtDesc(ProcessingJob.JobStatus status);
    long countByStatus(ProcessingJob.JobStatus status);
    long countByStatusAndCompletedAtAfter(ProcessingJob.JobStatus status, Instant since);
}
