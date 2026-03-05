package net.muttcode.spring.service;

import net.muttcode.spring.model.File;
import net.muttcode.spring.model.ProcessedFile;
import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.repository.FileRepository;
import net.muttcode.spring.repository.ProcessedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {

    @Mock
    private UpscaylService upscaylService;

    @Mock
    private FileService fileService;

    @Mock
    private ProcessingJobService jobService;

    @Mock
    private JobQueueService jobQueueService;

    @Mock
    private ProcessedFileRepository processedFileRepository;

    @Mock
    private FileRepository fileRepository;

    private ImageProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ImageProcessingService(
            upscaylService,
            fileService,
            jobService,
            jobQueueService,
            processedFileRepository,
            fileRepository
        );
    }

    @Test
    void submitJob_shouldCreateJobAndReturnJobId() throws Exception {
        String inputFileId = "test-file-id";
        Integer scaleFactor = 4;
        String modelName = "ultramix_balanced";

        // Mock enqueueJob to return a job ID
        when(jobQueueService.enqueueJob(anyString(), anyInt(), anyString())).thenReturn("queued-job-id");

        String jobId = service.submitJob(inputFileId, scaleFactor, modelName);

        assertNotNull(jobId);
        verify(jobService, times(1)).saveJob(any(ProcessingJob.class));
        verify(jobQueueService, times(1)).enqueueJob(inputFileId, scaleFactor, modelName);
    }

    @Test
    void getJobStatus_shouldReturnJobWhenExists() {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, "input-file-id", 2, "model");

        when(jobService.getJob(jobId)).thenReturn(Optional.of(job));

        ProcessingJob result = service.getJobStatus(jobId);

        assertNotNull(result);
        assertEquals(jobId, result.getJobId());
    }

    @Test
    void getJobStatus_shouldReturnNullWhenNotExists() {
        String jobId = UUID.randomUUID().toString();
        when(jobService.getJob(jobId)).thenReturn(Optional.empty());

        ProcessingJob result = service.getJobStatus(jobId);

        assertNull(result);
    }

    @Test
    void processedFileEntity_shouldBeCreatedCorrectly() {
        // This test verifies the ProcessedFile entity is created correctly for integration
        String inputFileId = "test-input-file";

        // Create input file entity
        File inputFile = new File(inputFileId, "input.png", "stored_input.png", 1024L, "image/png");

        // Create ProcessedFile entity
        ProcessedFile processedFile = new ProcessedFile(
            inputFile,
            "output-file-id",
            "output.png",
            ProcessedFile.ProcessingType.UPSCALE
        );
        processedFile.setFileSize(2048L);
        processedFile.setContentType("image/png");
        processedFile.setStatus(ProcessedFile.ProcessedFileStatus.COMPLETED);

        // Verify entity properties
        assertNotNull(processedFile);
        assertEquals(inputFile, processedFile.getFile());
        assertEquals("output.png", processedFile.getProcessedName());
        assertEquals(ProcessedFile.ProcessingType.UPSCALE, processedFile.getProcessingType());
        assertEquals(ProcessedFile.ProcessedFileStatus.COMPLETED, processedFile.getStatus());
        assertEquals(2048L, processedFile.getFileSize());
        assertEquals("image/png", processedFile.getContentType());
    }
}
