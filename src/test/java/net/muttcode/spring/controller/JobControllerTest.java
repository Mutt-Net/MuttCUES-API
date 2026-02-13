package net.muttcode.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.service.ProcessingJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessingJobService jobService;

    @MockBean
    private net.muttcode.spring.service.ImageProcessingService imageProcessingService;

    @Test
    void getJobStatus_shouldReturnJobWhenExists() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        job.setStatus(ProcessingJob.JobStatus.COMPLETED);
        
        when(jobService.getJob(jobId)).thenReturn(Optional.of(job));
        
        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(jobId))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getJobStatus_shouldReturn404WhenNotExists() throws Exception {
        String jobId = UUID.randomUUID().toString();
        
        when(jobService.getJob(jobId)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllJobs_shouldReturnJobList() throws Exception {
        ProcessingJob job1 = new ProcessingJob(UUID.randomUUID().toString(), "input-1", 2, "model1");
        ProcessingJob job2 = new ProcessingJob(UUID.randomUUID().toString(), "input-2", 4, "model2");
        
        when(jobService.getAllJobs()).thenReturn(List.of(job1, job2));
        
        mockMvc.perform(get("/api/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllJobs_withStatusFilter_shouldReturnFilteredList() throws Exception {
        ProcessingJob job = new ProcessingJob(UUID.randomUUID().toString(), "input-1", 2, "model1");
        job.setStatus(ProcessingJob.JobStatus.QUEUED);
        
        when(jobService.getJobsByStatus(ProcessingJob.JobStatus.QUEUED)).thenReturn(List.of(job));
        
        mockMvc.perform(get("/api/jobs").param("status", "QUEUED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("QUEUED"));
    }

    @Test
    void getStatistics_shouldReturnJobCounts() throws Exception {
        when(jobService.getAllJobs()).thenReturn(List.of(
            createJob(ProcessingJob.JobStatus.QUEUED),
            createJob(ProcessingJob.JobStatus.PROCESSING),
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.FAILED)
        ));
        
        mockMvc.perform(get("/api/jobs/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(5))
            .andExpect(jsonPath("$.queued").value(1))
            .andExpect(jsonPath("$.processing").value(1))
            .andExpect(jsonPath("$.completed").value(2))
            .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void getJobHistory_shouldReturnPaginatedResults() throws Exception {
        when(jobService.getAllJobs()).thenReturn(List.of(
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.FAILED)
        ));
        
        mockMvc.perform(get("/api/jobs/history").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobs.length()").value(2))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2));
    }

    private ProcessingJob createJob(ProcessingJob.JobStatus status) {
        ProcessingJob job = new ProcessingJob(UUID.randomUUID().toString(), "input-id", 2, "model");
        job.setStatus(status);
        return job;
    }
}
