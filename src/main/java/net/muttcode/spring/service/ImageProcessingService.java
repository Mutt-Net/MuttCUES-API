package net.muttcode.spring.service;

import net.muttcode.spring.model.ProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    private final ProcessingJobService jobService;
    private final JobProcessor jobProcessor;

    public ImageProcessingService(ProcessingJobService jobService,
                                  JobProcessor jobProcessor) {
        this.jobService = jobService;
        this.jobProcessor = jobProcessor;
    }

    public String submitJob(String inputFileId, Integer scaleFactor, String modelName) throws IOException {
        String jobId = UUID.randomUUID().toString();

        ProcessingJob job = new ProcessingJob(jobId, inputFileId, scaleFactor, modelName);
        jobService.saveJob(job);

        // Hand off to a SEPARATE bean so @Async is honoured (a same-bean call would
        // run synchronously and block this request thread for the whole upscale).
        // The old jobQueueService.enqueueJob() call was removed: it pushed to a Redis
        // list nothing ever dequeued (pure backlog) and minted a duplicate job row.
        jobProcessor.processJobAsync(jobId);

        return jobId;
    }

    public ProcessingJob getJobStatus(String jobId) {
        return jobService.getJob(jobId).orElse(null);
    }
}
