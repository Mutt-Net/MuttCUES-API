package net.muttcode.spring.service;

import net.muttcode.spring.model.ProcessingJob;
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

    @Value("${upscayl.input.mount:/app/input}")
    private String upscaylInputMount;

    @Value("${upscayl.output.mount:/app/output}")
    private String upscaylOutputMount;

    public ImageProcessingService(UpscaylService upscaylService, 
                                  FileService fileService,
                                  ProcessingJobService jobService,
                                  JobQueueService jobQueueService) {
        this.upscaylService = upscaylService;
        this.fileService = fileService;
        this.jobService = jobService;
        this.jobQueueService = jobQueueService;
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

    public ProcessingJob getJobStatus(String jobId) {
        return jobService.getJob(jobId).orElse(null);
    }
}
