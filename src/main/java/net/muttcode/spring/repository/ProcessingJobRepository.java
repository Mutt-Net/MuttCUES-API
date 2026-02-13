package net.muttcode.spring.repository;

import net.muttcode.spring.model.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, String> {
    List<ProcessingJob> findByStatusOrderByCreatedAtDesc(ProcessingJob.JobStatus status);
    List<ProcessingJob> findAllByOrderByCreatedAtDesc();
}
