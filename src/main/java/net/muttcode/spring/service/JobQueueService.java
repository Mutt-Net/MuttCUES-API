package net.muttcode.spring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.muttcode.spring.model.ProcessingJob;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JobQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProcessingJobService jobService;
    private final ObjectMapper objectMapper;

    private static final String JOB_QUEUE = "job:queue";
    private static final String JOB_STATUS_PREFIX = "job:status:";
    private static final long QUEUE_TTL_HOURS = 24;

    public JobQueueService(RedisConnectionFactory connectionFactory, 
                           ProcessingJobService jobService,
                           ObjectMapper objectMapper) {
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(connectionFactory);
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        this.redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        this.redisTemplate.afterPropertiesSet();
        
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    public String enqueueJob(String inputFileId, Integer scaleFactor, String modelName) {
        String jobId = UUID.randomUUID().toString();
        
        ProcessingJob job = new ProcessingJob(jobId, inputFileId, scaleFactor, modelName);
        jobService.saveJob(job);
        
        JobMessage message = new JobMessage(jobId, inputFileId, scaleFactor, modelName);
        
        redisTemplate.opsForList().rightPush(JOB_QUEUE, jobId);
        redisTemplate.opsForHash().put(JOB_STATUS_PREFIX + jobId, "status", "QUEUED");
        redisTemplate.expire(JOB_STATUS_PREFIX + jobId, QUEUE_TTL_HOURS, TimeUnit.HOURS);
        
        return jobId;
    }

    public String dequeueJob() {
        String jobId = (String) redisTemplate.opsForList().leftPop(JOB_QUEUE);
        return jobId;
    }

    public void updateJobStatus(String jobId, String status, Integer progress) {
        redisTemplate.opsForHash().put(JOB_STATUS_PREFIX + jobId, "status", status);
        if (progress != null) {
            redisTemplate.opsForHash().put(JOB_STATUS_PREFIX + jobId, "progress", progress.toString());
        }
    }

    public Object getJobStatus(String jobId) {
        return redisTemplate.opsForHash().entries(JOB_STATUS_PREFIX + jobId);
    }

    public static class JobMessage {
        private String jobId;
        private String inputFileId;
        private Integer scaleFactor;
        private String modelName;

        public JobMessage() {}

        public JobMessage(String jobId, String inputFileId, Integer scaleFactor, String modelName) {
            this.jobId = jobId;
            this.inputFileId = inputFileId;
            this.scaleFactor = scaleFactor;
            this.modelName = modelName;
        }

        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public String getInputFileId() { return inputFileId; }
        public void setInputFileId(String inputFileId) { this.inputFileId = inputFileId; }
        public Integer getScaleFactor() { return scaleFactor; }
        public void setScaleFactor(Integer scaleFactor) { this.scaleFactor = scaleFactor; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
    }
}
