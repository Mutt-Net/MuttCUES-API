package net.muttcode.spring.controller;

import net.muttcode.spring.model.ProcessedFile;
import net.muttcode.spring.service.DdsConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/convert")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DdsConversionController {
    
    private static final Logger logger = Logger.getLogger(DdsConversionController.class.getName());
    private final DdsConversionService ddsConversionService;
    
    public DdsConversionController(DdsConversionService ddsConversionService) {
        this.ddsConversionService = ddsConversionService;
    }
    
    @PostMapping("/dds-to-png")
    public ResponseEntity<Map<String, Object>> convertDdsToPng(@RequestParam("file") MultipartFile file) {
        logger.info("DDS to PNG conversion request: " + file.getOriginalFilename());
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is empty"));
            }
            
            ProcessedFile result = ddsConversionService.ddsToPng(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileId", result.getFileId());
            response.put("fileName", result.getFileName());
            response.put("downloadUrl", "/api/convert/" + result.getFileId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Conversion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/image-to-dds")
    public ResponseEntity<Map<String, Object>> convertImageToDds(@RequestParam("file") MultipartFile file) {
        logger.info("Image to DDS conversion request: " + file.getOriginalFilename());
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is empty"));
            }
            
            ProcessedFile result = ddsConversionService.imageToDds(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileId", result.getFileId());
            response.put("fileName", result.getFileName());
            response.put("downloadUrl", "/api/convert/" + result.getFileId());
            response.put("format", "Uncompressed ARGB");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Conversion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadConvertedFile(@PathVariable String fileId) {
        try {
            Path outputDir = ddsConversionService.getOutputPath();
            
            Path[] files = Files.list(outputDir)
                .filter(p -> p.getFileName().toString().startsWith(fileId))
                .toArray(Path[]::new);
            
            if (files.length == 0) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(files[0].toUri());
            String contentType = resource.getFilename().endsWith(".png") ? "image/png" : "image/vnd.ms-dds";
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
                
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "DDS Converter");
    }
}
