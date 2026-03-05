package net.muttcode.spring.service;

import net.muttcode.spring.model.File;
import net.muttcode.spring.model.ProcessedFile;
import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.repository.FileRepository;
import net.muttcode.spring.repository.ProcessedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    private final UpscaylService upscaylService;
    private final FileService fileService;
    private final ProcessingJobService jobService;
    private final JobQueueService jobQueueService;
    private final ProcessedFileRepository processedFileRepository;
    private final FileRepository fileRepository;

    @Value("${upscayl.input.mount:/app/input}")
    private String upscaylInputMount;

    @Value("${upscayl.output.mount:/app/output}")
    private String upscaylOutputMount;

    public ImageProcessingService(UpscaylService upscaylService, 
                                  FileService fileService,
                                  ProcessingJobService jobService,
                                  JobQueueService jobQueueService,
                                  ProcessedFileRepository processedFileRepository,
                                  FileRepository fileRepository) {
        this.upscaylService = upscaylService;
        this.fileService = fileService;
        this.jobService = jobService;
        this.jobQueueService = jobQueueService;
        this.processedFileRepository = processedFileRepository;
        this.fileRepository = fileRepository;
    }

    public String submitJob(String inputFileId, Integer scaleFactor, String modelName) throws IOException {
        String jobId = UUID.randomUUID().toString();
        
        ProcessingJob job = new ProcessingJob(jobId, inputFileId, scaleFactor, modelName);
        jobService.saveJob(job);
        
        jobQueueService.enqueueJob(inputFileId, scaleFactor, modelName);
        
        processJobAsync(jobId);
        
        return jobId;
    }

    @Async
    public void processJobAsync(String jobId) {
        logger.info("Processing job {} asynchronously", jobId);
        
        ProcessingJob job = jobService.getJob(jobId).orElse(null);
        if (job == null) {
            logger.error("Job not found: {}", jobId);
            return;
        }

        try {
            jobService.startProcessing(jobId);
            
            Path inputPath = fileService.getFilePath(job.getInputFileId());
            
            Path upscaylInput = Path.of(upscaylInputMount, job.getInputFileId());
            Files.createDirectories(upscaylInput.getParent());
            Files.copy(inputPath, upscaylInput, StandardCopyOption.REPLACE_EXISTING);
            
            long startTime = System.currentTimeMillis();
            
            UpscaylService.UpscaylResult result = upscaylService.processImage(
                upscaylInput.toString(),
                job.getScaleFactor(),
                job.getModelName()
            );
            
            long processingTime = System.currentTimeMillis() - startTime;

            if (result.isSuccess()) {
                String outputFileName = Path.of(result.getOutputPath()).getFileName().toString();
                String outputFileId = UUID.randomUUID().toString() + "_" + outputFileName;
                
                Path outputPath = fileService.getFilePath(outputFileId);
                Path upscaylOutput = Path.of(upscaylOutputMount, outputFileName);
                
                if (Files.exists(upscaylOutput)) {
                    Files.copy(upscaylOutput, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    
                    // Create ProcessedFile record for the output
                    createProcessedFileRecord(job, outputFileId, outputFileName, outputPath, processingTime);
                    
                    jobService.completeJob(jobId, outputFileId, processingTime);
                    logger.info("Job {} completed successfully in {}ms", jobId, processingTime);
                } else {
                    jobService.failJob(jobId, "Output file not found after processing");
                    logger.error("Job {} failed: output file not found", jobId);
                }
            } else {
                jobService.failJob(jobId, result.getError());
                logger.error("Job {} failed: {}", jobId, result.getError());
            }
            
            Files.deleteIfExists(upscaylInput);
            
        } catch (Exception e) {
            logger.error("Job {} failed with exception", jobId, e);
            jobService.failJob(jobId, e.getMessage());
        }
    }

    /**
     * Creates a ProcessedFile record linking the output file to the original input file.
     */
    private void createProcessedFileRecord(ProcessingJob job, String outputFileId, String outputFileName, Path outputPath, long processingTimeMs) {
        try {
            // Look up the original File entity
            File inputFile = fileRepository.findByFileId(job.getInputFileId())
                .orElseThrow(() -> new IllegalStateException("Input file not found: " + job.getInputFileId()));

            // Get output file metadata
            long outputFileSize = Files.size(outputPath);
            String outputContentType = Files.probeContentType(outputPath);

            // Create ProcessedFile entity
            ProcessedFile processedFile = new ProcessedFile(
                inputFile,
                outputFileId,
                outputFileName,
                ProcessedFile.ProcessingType.UPSCALE
            );
            processedFile.setFileSize(outputFileSize);
            processedFile.setContentType(outputContentType);
            processedFile.setStatus(ProcessedFile.ProcessedFileStatus.COMPLETED);
            processedFile.setProcessingStartedAt(Instant.now().minusMillis(processingTimeMs));
            processedFile.setProcessingCompletedAt(Instant.now());

            processedFileRepository.save(processedFile);
            logger.info("Created ProcessedFile record {} for job {}", processedFile.getId(), job.getJobId());

        } catch (Exception e) {
            logger.error("Failed to create ProcessedFile record for job {}", job.getJobId(), e);
            // Don't fail the job - the processed file was created successfully
        }
    }

    public ProcessingJob getJobStatus(String jobId) {
        return jobService.getJob(jobId).orElse(null);
    }
}
