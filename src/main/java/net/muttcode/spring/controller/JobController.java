package net.muttcode.spring.controller;

import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.service.FileService;
import net.muttcode.spring.service.ImageProcessingService;
import net.muttcode.spring.service.JobQueueService;
import net.muttcode.spring.service.ProcessingJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final ImageProcessingService imageProcessingService;
    private final ProcessingJobService jobService;
    private final FileService fileService;
    private final JobQueueService jobQueueService;

    public JobController(ImageProcessingService imageProcessingService, 
                         ProcessingJobService jobService,
                         FileService fileService,
                         JobQueueService jobQueueService) {
        this.imageProcessingService = imageProcessingService;
        this.jobService = jobService;
        this.fileService = fileService;
        this.jobQueueService = jobQueueService;
    }

    @PostMapping("/process")
    public Map<String, Object> submitJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "scaleFactor", defaultValue = "2") Integer scaleFactor,
            @RequestParam(value = "modelName", defaultValue = "ultramix_balanced") String modelName) throws IOException {
        
        var stored = fileService.saveFile(file);
        String jobId = imageProcessingService.submitJob(stored.getFileId(), scaleFactor, modelName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("inputFileId", stored.getFileId());
        response.put("status", "QUEUED");
        response.put("scaleFactor", scaleFactor);
        response.put("modelName", modelName);
        
        return response;
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        return jobService.getJob(jobId)
                .map(job -> ResponseEntity.ok(jobToMap(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Map<String, Object>> getAllJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        
        List<ProcessingJob> jobs;
        if (status != null && !status.isEmpty()) {
            try {
                ProcessingJob.JobStatus jobStatus = ProcessingJob.JobStatus.valueOf(status.toUpperCase());
                jobs = jobService.getJobsByStatus(jobStatus);
            } catch (IllegalArgumentException e) {
                jobs = jobService.getAllJobs();
            }
        } else {
            jobs = jobService.getAllJobs();
        }
        
        return jobs.stream()
                .limit(limit)
                .map(this::jobToMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/history")
    public Map<String, Object> getJobHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<ProcessingJob> allJobs = jobService.getAllJobs();
        
        int start = page * size;
        int end = Math.min(start + size, allJobs.size());
        
        List<ProcessingJob> pageJobs = start < allJobs.size() 
                ? allJobs.subList(start, end) 
                : List.of();
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobs", pageJobs.stream().map(this::jobToMap).collect(Collectors.toList()));
        response.put("page", page);
        response.put("size", size);
        response.put("total", allJobs.size());
        response.put("totalPages", (allJobs.size() + size - 1) / size);
        
        return response;
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        List<ProcessingJob> allJobs = jobService.getAllJobs();
        
        long queued = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingJob.JobStatus.QUEUED).count();
        long processing = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingJob.JobStatus.PROCESSING).count();
        long completed = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingJob.JobStatus.COMPLETED).count();
        long failed = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingJob.JobStatus.FAILED).count();
        
        double avgProcessingTime = allJobs.stream()
                .filter(j -> j.getProcessingTimeMs() != null)
                .mapToLong(ProcessingJob::getProcessingTimeMs)
                .average()
                .orElse(0.0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", allJobs.size());
        stats.put("queued", queued);
        stats.put("processing", processing);
        stats.put("completed", completed);
        stats.put("failed", failed);
        stats.put("successRate", completed > 0 ? (double) completed / (completed + failed) * 100 : 0);
        stats.put("averageProcessingTimeMs", avgProcessingTime);
        
        return stats;
    }

    @GetMapping("/queue/status")
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("queue", "available");
        return status;
    }

    private Map<String, Object> jobToMap(ProcessingJob job) {
        Map<String, Object> map = new HashMap<>();
        map.put("jobId", job.getJobId());
        map.put("inputFileId", job.getInputFileId());
        map.put("outputFileId", job.getOutputFileId());
        map.put("status", job.getStatus().name());
        map.put("scaleFactor", job.getScaleFactor());
        map.put("modelName", job.getModelName());
        map.put("progressPercent", job.getProgressPercent());
        map.put("errorMessage", job.getErrorMessage());
        map.put("createdAt", job.getCreatedAt());
        map.put("startedAt", job.getStartedAt());
        map.put("completedAt", job.getCompletedAt());
        map.put("processingTimeMs", job.getProcessingTimeMs());
        return map;
    }
}
