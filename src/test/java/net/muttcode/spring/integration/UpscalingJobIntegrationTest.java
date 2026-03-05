package net.muttcode.spring.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.muttcode.spring.controller.JobController;
import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.service.FileService;
import net.muttcode.spring.service.ImageProcessingService;
import net.muttcode.spring.service.JobQueueService;
import net.muttcode.spring.service.ProcessingJobService;
import net.muttcode.spring.service.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(JobController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UpscalingJobIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImageProcessingService imageProcessingService;

    @MockBean
    private ProcessingJobService jobService;

    @MockBean
    private FileService fileService;

    @MockBean
    private JobQueueService jobQueueService;

    @MockBean
    private net.muttcode.spring.service.JwtService jwtService;

    @MockBean
    private net.muttcode.spring.service.CustomUserDetailsService customUserDetailsService;

    private Path testUploadDir;

    @BeforeEach
    void setUp() throws Exception {
        testUploadDir = Files.createTempDirectory("test-uploads");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testUploadDir != null && Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    @Test
    void submitUpscalingJob_shouldReturnJobIdAndQueuedStatus() throws Exception {
        byte[] fileContent = "Test image content".getBytes();
        MockMultipartFile testFile = new MockMultipartFile(
            "file",
            "test-image.png",
            "image/png",
            fileContent
        );

        String inputFileId = "input-file-id-123";
        String jobId = "job-id-456";
        Integer scaleFactor = 2;
        String modelName = "ultramix_balanced";

        StoredFile mockStoredFile = new StoredFile(inputFileId, "test-image.png", inputFileId + "_test-image.png", testUploadDir);
        when(fileService.saveFile(any(MockMultipartFile.class))).thenReturn(mockStoredFile);
        when(imageProcessingService.submitJob(anyString(), anyInt(), anyString())).thenReturn(jobId);

        mockMvc.perform(multipart("/api/jobs/process")
                .file(testFile)
                .param("scaleFactor", String.valueOf(scaleFactor))
                .param("modelName", modelName))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(jobId))
            .andExpect(jsonPath("$.inputFileId").value(inputFileId))
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.scaleFactor").value(scaleFactor))
            .andExpect(jsonPath("$.modelName").value(modelName));

        verify(fileService, times(1)).saveFile(any(MockMultipartFile.class));
        verify(imageProcessingService, times(1)).submitJob(inputFileId, scaleFactor, modelName);
    }

    @Test
    void getJobStatus_shouldReturnQueuedStatus() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob queuedJob = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        queuedJob.setStatus(ProcessingJob.JobStatus.QUEUED);
        when(jobService.getJob(jobId)).thenReturn(Optional.of(queuedJob));

        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(jobId))
            .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void getJobStatus_shouldReturnProcessingStatus() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 4, "realesrgan-x4plus");
        job.setStatus(ProcessingJob.JobStatus.PROCESSING);
        job.setProgressPercent(50);
        when(jobService.getJob(jobId)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.progressPercent").value(50));
    }

    @Test
    void getJobStatus_shouldReturnCompletedStatus() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        job.setStatus(ProcessingJob.JobStatus.COMPLETED);
        job.setOutputFileId("output-file-id.png");
        job.setProcessingTimeMs(5000L);
        when(jobService.getJob(jobId)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.outputFileId").value("output-file-id.png"))
            .andExpect(jsonPath("$.processingTimeMs").value(5000));
    }

    @Test
    void getJobStatus_shouldReturnFailedStatus() throws Exception {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "ultramix_balanced");
        job.setStatus(ProcessingJob.JobStatus.FAILED);
        job.setErrorMessage("Upscayl service unavailable");
        when(jobService.getJob(jobId)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorMessage").value("Upscayl service unavailable"));
    }

    @Test
    void getJobStatus_shouldReturn404ForNonExistentJob() throws Exception {
        when(jobService.getJob("non-existent")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/jobs/{jobId}", "non-existent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getStatistics_shouldReturnJobCounts() throws Exception {
        when(jobService.getAllJobs()).thenReturn(java.util.List.of(
            createJob(ProcessingJob.JobStatus.QUEUED),
            createJob(ProcessingJob.JobStatus.PROCESSING),
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.FAILED)
        ));

        mockMvc.perform(get("/api/jobs/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(5))
            .andExpect(jsonPath("$.completed").value(2))
            .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void getJobHistory_shouldReturnPaginatedResults() throws Exception {
        when(jobService.getAllJobs()).thenReturn(java.util.List.of(
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.COMPLETED),
            createJob(ProcessingJob.JobStatus.FAILED)
        ));

        mockMvc.perform(get("/api/jobs/history").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobs.length()").value(2))
            .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void getQueueStatus_shouldReturnAvailable() throws Exception {
        mockMvc.perform(get("/api/jobs/queue/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queue").value("available"));
    }

    private ProcessingJob createJob(ProcessingJob.JobStatus status) {
        ProcessingJob job = new ProcessingJob(UUID.randomUUID().toString(), "input-id", 2, "model");
        job.setStatus(status);
        return job;
    }
}
