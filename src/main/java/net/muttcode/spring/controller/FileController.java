package net.muttcode.spring.controller;

import net.muttcode.spring.service.FileService;
import net.muttcode.spring.service.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // -------- UPLOAD --------
    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {

        StoredFile stored = fileService.saveFile(file);

        return Map.of(
                "fileId", stored.getFileId(),
                "fileName", stored.getStoredName()
        );
    }

    // -------- DOWNLOAD --------
    @GetMapping("/download/{fileId}")
public ResponseEntity<Resource> download(@PathVariable String fileId) throws IOException {

    Path filePath = fileService.getFilePath(fileId);

    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists() || !resource.isReadable()) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
}
}v