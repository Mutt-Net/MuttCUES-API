package net.muttcode.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UpscaylService {

    private static final Logger logger = LoggerFactory.getLogger(UpscaylService.class);

    @Value("${upscayl.service.url:http://upscayl:8081}")
    private String upscaylServiceUrl;

    @Value("${upscayl.models.path:/app/models}")
    private String modelsPath;

    @Value("${upscayl.input.path:/app/input}")
    private String inputPath;

    @Value("${upscayl.output.path:/app/output}")
    private String outputPath;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public UpscaylService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public UpscaylResult processImage(String inputFilePath, Integer scaleFactor, String modelName) throws IOException {
        String jobId = UUID.randomUUID().toString();
        
        logger.info("Starting upscalce job {} with scale={}, model={}", jobId, scaleFactor, modelName);

        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            throw new IOException("Input file not found: " + inputFilePath);
        }

        String inputFileName = inputFile.getName();
        String outputFileName = getOutputFileName(inputFileName, scaleFactor);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input", "/app/input/" + inputFileName);
        requestBody.put("output", "/app/output/" + outputFileName);
        requestBody.put("model", modelName);
        requestBody.put("scale", scaleFactor);
        requestBody.put("gpu", true);

        try {
            String apiUrl = upscaylServiceUrl + "/api/upscale";
            logger.debug("Calling Upscayl API: {}", apiUrl);

            Map<String, Object> response = restTemplate.postForObject(apiUrl, requestBody, Map.class);
            
            logger.info("Upscayl response: {}", response);

            if (response != null && "success".equals(response.get("status"))) {
                String outputPathResult = (String) response.get("output");
                return new UpscaylResult(true, outputPathResult, null);
            } else {
                String errorMsg = response != null ? (String) response.get("error") : "Unknown error";
                return new UpscaylResult(false, null, errorMsg);
            }
        } catch (Exception e) {
            logger.error("Upscayl processing failed", e);
            return new UpscaylResult(false, null, e.getMessage());
        }
    }

    public UpscaylResult processImageAsync(String inputFilePath, Integer scaleFactor, String modelName) throws IOException {
        String jobId = UUID.randomUUID().toString();
        
        logger.info("Starting async upscalce job {} with scale={}, model={}", jobId, scaleFactor, modelName);

        File inputFile = new File(inputFilePath);
        if (!inputFile.exists()) {
            throw new IOException("Input file not found: " + inputFilePath);
        }

        String inputFileName = inputFile.getName();
        String outputFileName = getOutputFileName(inputFileName, scaleFactor);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("jobId", jobId);
        requestBody.put("input", "/app/input/" + inputFileName);
        requestBody.put("output", "/app/output/" + outputFileName);
        requestBody.put("model", modelName);
        requestBody.put("scale", scaleFactor);
        requestBody.put("gpu", true);

        try {
            String apiUrl = upscaylServiceUrl + "/api/upscale";
            logger.debug("Calling Upscayl API: {}", apiUrl);

            Map<String, Object> response = restTemplate.postForObject(apiUrl, requestBody, Map.class);
            
            logger.info("Upscayl response: {}", response);

            if (response != null && "success".equals(response.get("status"))) {
                String outputPathResult = (String) response.get("output");
                return new UpscaylResult(true, outputPathResult, null);
            } else {
                String errorMsg = response != null ? (String) response.get("error") : "Unknown error";
                return new UpscaylResult(false, null, errorMsg);
            }
        } catch (Exception e) {
            logger.error("Upscayl processing failed", e);
            return new UpscaylResult(false, null, e.getMessage());
        }
    }

    public Map<String, Object> getJobStatus(String jobId) {
        try {
            String apiUrl = upscaylServiceUrl + "/api/job/" + jobId;
            return restTemplate.getForObject(apiUrl, Map.class);
        } catch (Exception e) {
            logger.error("Failed to get job status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return error;
        }
    }

    public String[] getAvailableModels() {
        try {
            String apiUrl = upscaylServiceUrl + "/api/models";
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
            if (response != null && response.containsKey("models")) {
                return (String[]) response.get("models");
            }
        } catch (Exception e) {
            logger.warn("Failed to get available models", e);
        }
        return new String[]{"ultramix_balanced", "realesrgan-x4plus", "realcugan"};
    }

    private String getOutputFileName(String inputFileName, Integer scaleFactor) {
        String baseName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
        String extension = inputFileName.substring(inputFileName.lastIndexOf('.'));
        return baseName + "_" + scaleFactor + "x" + extension;
    }

    public static class UpscaylResult {
        private final boolean success;
        private final String outputPath;
        private final String error;

        public UpscaylResult(boolean success, String outputPath, String error) {
            this.success = success;
            this.outputPath = outputPath;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getOutputPath() { return outputPath; }
        public String getError() { return error; }
    }
}
