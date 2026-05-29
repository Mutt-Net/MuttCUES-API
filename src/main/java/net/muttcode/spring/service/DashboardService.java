package net.muttcode.spring.service;

import net.muttcode.spring.model.ProcessingJob;
import net.muttcode.spring.repository.ProcessingJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private static final int STUCK_THRESHOLD_MINUTES = 30;
    private static final int QUEUE_ALERT_DEPTH = 10;

    private final ProcessingJobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Value("${upscayl.models.path:/app/models}")
    private String modelsPath;

    @Value("${file.upload-dir:./uploads}")
    private String uploadsDir;

    @Value("${upscayl.output.path:/app/output}")
    private String outputDir;

    private List<String> cachedModels;

    public DashboardService(ProcessingJobRepository jobRepository, JobQueueService jobQueueService) {
        this.jobRepository = jobRepository;
        this.jobQueueService = jobQueueService;
    }

    public Map<String, Object> getDashboardSummary() {
        Instant now = Instant.now();
        Instant stuckCutoff = now.minus(STUCK_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        Instant since24h = now.minus(24, ChronoUnit.HOURS);

        List<Map<String, String>> alerts = new ArrayList<>();

        List<ProcessingJob> stuckJobs = jobRepository.findByStatusAndCreatedAtBefore(
                ProcessingJob.JobStatus.QUEUED, stuckCutoff);
        for (ProcessingJob job : stuckJobs) {
            long minutesWaiting = ChronoUnit.MINUTES.between(job.getCreatedAt(), now);
            Map<String, String> alert = new HashMap<>();
            alert.put("title", "Job stuck in queue");
            alert.put("detail", job.getJobId().substring(0, 8) + "… > " + minutesWaiting + " min");
            alert.put("severity", "urgent");
            alerts.add(alert);
        }

        long failedCount = jobRepository.countByStatusAndCreatedAtAfter(
                ProcessingJob.JobStatus.FAILED, since24h);
        if (failedCount > 0) {
            Map<String, String> alert = new HashMap<>();
            alert.put("title", failedCount + " job" + (failedCount > 1 ? "s" : "") + " failed in last 24h");
            alert.put("detail", "Check /api/status for details");
            alert.put("severity", "urgent");
            alerts.add(alert);
        }

        long queueDepth = jobQueueService.getQueueDepth();
        if (queueDepth > QUEUE_ALERT_DEPTH) {
            Map<String, String> alert = new HashMap<>();
            alert.put("title", "Queue depth: " + queueDepth);
            alert.put("detail", "Processing may be delayed");
            alert.put("severity", "info");
            alerts.add(alert);
        }

        List<Map<String, String>> recent = new ArrayList<>();
        List<ProcessingJob> recentJobs = jobRepository.findTop10ByStatusOrderByCompletedAtDesc(
                ProcessingJob.JobStatus.COMPLETED);
        for (ProcessingJob job : recentJobs.subList(0, Math.min(5, recentJobs.size()))) {
            Map<String, String> item = new HashMap<>();
            String filename = job.getInputFileId().length() > 12
                    ? job.getInputFileId().substring(0, 12) + "…"
                    : job.getInputFileId();
            item.put("tool", "MuttCUES");
            item.put("text", "upscaled " + filename + " " + job.getScaleFactor() + "x — " + job.getModelName());
            item.put("timestamp", job.getCompletedAt() != null ? job.getCompletedAt().toString() : "");
            recent.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("alerts", alerts);
        result.put("recent", recent);
        result.put("upcoming", List.of());
        return result;
    }

    public Map<String, Object> getStatus() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);

        long queueDepth = jobQueueService.getQueueDepth();
        long running = jobRepository.countByStatus(ProcessingJob.JobStatus.PROCESSING);
        long completed24h = jobRepository.countByStatusAndCreatedAtAfter(
                ProcessingJob.JobStatus.COMPLETED, since24h);
        long failed24h = jobRepository.countByStatusAndCreatedAtAfter(
                ProcessingJob.JobStatus.FAILED, since24h);

        List<ProcessingJob> recentJobs = jobRepository.findTop10ByStatusOrderByCompletedAtDesc(
                ProcessingJob.JobStatus.COMPLETED);

        List<Map<String, Object>> recentJobsList = new ArrayList<>();
        for (ProcessingJob job : recentJobs) {
            Map<String, Object> j = new HashMap<>();
            j.put("jobId", job.getJobId());
            j.put("status", job.getStatus().name());
            j.put("modelName", job.getModelName());
            j.put("scaleFactor", job.getScaleFactor());
            j.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
            recentJobsList.add(j);
        }

        Map<String, Object> jobs = new HashMap<>();
        jobs.put("completed_24h", completed24h);
        jobs.put("failed_24h", failed24h);
        jobs.put("running", running);

        Map<String, Object> storage = new HashMap<>();
        storage.put("uploads_mb", dirSizeMb(uploadsDir));
        storage.put("outputs_mb", dirSizeMb(outputDir));

        Map<String, Object> result = new HashMap<>();
        result.put("queue_depth", queueDepth);
        result.put("jobs", jobs);
        result.put("recent_jobs", recentJobsList);
        result.put("models", getModels());
        result.put("storage", storage);
        return result;
    }

    List<String> getModels() {
        if (cachedModels != null) return cachedModels;
        Path models = Path.of(modelsPath);
        if (!Files.isDirectory(models)) {
            cachedModels = List.of();
            return cachedModels;
        }
        try (Stream<Path> paths = Files.list(models)) {
            cachedModels = paths
                    .filter(p -> p.toString().endsWith(".param") || Files.isDirectory(p))
                    .map(p -> p.getFileName().toString().replace(".param", ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return cachedModels;
    }

    private long dirSizeMb(String dir) {
        Path path = Path.of(dir);
        if (!Files.exists(path)) return 0L;
        try (Stream<Path> paths = Files.walk(path)) {
            long bytes = paths.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0L; }
                    }).sum();
            return bytes / (1024 * 1024);
        } catch (IOException e) {
            return 0L;
        }
    }
}
