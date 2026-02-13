package net.muttcode.spring.controller;

import net.muttcode.spring.service.FileService;
import net.muttcode.spring.service.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class FileController {

    private static final Logger logger = Logger.getLogger(FileController.class.getName());
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // -------- UPLOAD --------
    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {

        logger.info("Upload request received for file: " + file.getOriginalFilename());

        StoredFile stored;
        try {
            stored = fileService.saveFile(file);
            logger.info("File saved successfully. FileId: " + stored.getFileId() + ", StoredName: " + stored.getStoredName());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving file: " + file.getOriginalFilename(), e);
            throw e;  // rethrow to let Spring handle it
        }


        return Map.of(
                "fileId", stored.getFileId(),
                "fileName", stored.getStoredName()
        );
    }

    // -------- DOWNLOAD --------
    @GetMapping("/upload/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) throws IOException {
        logger.info("Download request received for fileId: " + fileId);

        Path filePath;
        Resource resource;
        try {
            filePath = fileService.getFilePath(fileId);
            resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.warning("File not found or not readable: " + filePath);
                return ResponseEntity.notFound().build();
            }

            logger.info("File found, preparing download: " + resource.getFilename());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error accessing file for fileId: " + fileId, e);
            throw e;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

}