package net.muttcode.spring.service;

import net.muttcode.spring.model.ProcessingJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.muttcode.spring.repository.ProcessingJobRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessingJobServiceTest {

    @Mock
    private ProcessingJobRepository repository;

    private ProcessingJobService service;

    @BeforeEach
    void setUp() {
        service = new ProcessingJobService(repository);
    }

    @Test
    void saveJob_shouldReturnSavedJob() {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        
        when(repository.save(any(ProcessingJob.class))).thenReturn(job);
        
        ProcessingJob result = service.saveJob(job);
        
        assertNotNull(result);
        assertEquals(jobId, result.getJobId());
        verify(repository, times(1)).save(job);
    }

    @Test
    void getJob_shouldReturnJobWhenExists() {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        
        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        
        Optional<ProcessingJob> result = service.getJob(jobId);
        
        assertTrue(result.isPresent());
        assertEquals(jobId, result.get().getJobId());
    }

    @Test
    void getJob_shouldReturnEmptyWhenNotExists() {
        String jobId = UUID.randomUUID().toString();
        
        when(repository.findById(jobId)).thenReturn(Optional.empty());
        
        Optional<ProcessingJob> result = service.getJob(jobId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllJobs_shouldReturnAllJobs() {
        ProcessingJob job1 = new ProcessingJob(UUID.randomUUID().toString(), "input-1", 2, "model1");
        ProcessingJob job2 = new ProcessingJob(UUID.randomUUID().toString(), "input-2", 4, "model2");
        
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(job1, job2));
        
        List<ProcessingJob> result = service.getAllJobs();
        
        assertEquals(2, result.size());
    }

    @Test
    void getJobsByStatus_shouldReturnFilteredJobs() {
        ProcessingJob job = new ProcessingJob(UUID.randomUUID().toString(), "input-1", 2, "model1");
        job.setStatus(ProcessingJob.JobStatus.QUEUED);
        
        when(repository.findByStatusOrderByCreatedAtDesc(ProcessingJob.JobStatus.QUEUED))
            .thenReturn(List.of(job));
        
        List<ProcessingJob> result = service.getJobsByStatus(ProcessingJob.JobStatus.QUEUED);
        
        assertEquals(1, result.size());
        assertEquals(ProcessingJob.JobStatus.QUEUED, result.get(0).getStatus());
    }

    @Test
    void startProcessing_shouldUpdateJobStatus() {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        job.setStatus(ProcessingJob.JobStatus.QUEUED);
        
        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(repository.save(any(ProcessingJob.class))).thenReturn(job);
        
        service.startProcessing(jobId);
        
        assertEquals(ProcessingJob.JobStatus.PROCESSING, job.getStatus());
        assertNotNull(job.getStartedAt());
        verify(repository, times(1)).save(job);
    }

    @Test
    void completeJob_shouldUpdateStatusAndTiming() {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        job.setStatus(ProcessingJob.JobStatus.PROCESSING);
        
        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(repository.save(any(ProcessingJob.class))).thenReturn(job);
        
        service.completeJob(jobId, "output-file-id", 5000L);
        
        assertEquals(ProcessingJob.JobStatus.COMPLETED, job.getStatus());
        assertEquals("output-file-id", job.getOutputFileId());
        assertEquals(5000L, job.getProcessingTimeMs());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    void failJob_shouldUpdateStatusWithError() {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        job.setStatus(ProcessingJob.JobStatus.PROCESSING);
        
        when(repository.findById(jobId)).thenReturn(Optional.of(job));
        when(repository.save(any(ProcessingJob.class))).thenReturn(job);
        
        service.failJob(jobId, "Test error message");
        
        assertEquals(ProcessingJob.JobStatus.FAILED, job.getStatus());
        assertEquals("Test error message", job.getErrorMessage());
        assertNotNull(job.getCompletedAt());
    }
}
