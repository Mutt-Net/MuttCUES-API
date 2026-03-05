package net.muttcode.spring.service;

import net.muttcode.spring.model.File;
import net.muttcode.spring.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileService {

    private final Path uploadDir;
    private final FileRepository fileRepository;

    public FileService(
            FileRepository fileRepository,
            @Value("${file.upload-dir:#{T(java.lang.System).getenv('UPLOAD_DIR') != null ? T(java.lang.System).getenv('UPLOAD_DIR') : './uploads'}}") String uploadDirStr) throws IOException {
        this.fileRepository = fileRepository;
        this.uploadDir = Paths.get(uploadDirStr);
        Files.createDirectories(uploadDir);
        System.out.println("Upload dir: " + uploadDir.toAbsolutePath());
    }

    public StoredFile saveFile(MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename();
        String storedName = fileId + "_" + originalName;

        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Create and save File entity to database
        File fileEntity = new File(fileId, originalName, storedName, file.getSize(), file.getContentType());
        fileRepository.save(fileEntity);

        return new StoredFile(fileId, originalName, storedName, target);
    }

    public Path getFilePath(String fileId) throws IOException {
        // Look up the File entity to get the storedName
        File file = fileRepository.findByFileId(fileId)
            .orElseThrow(() -> new IOException("File not found for fileId: " + fileId));
        
        return uploadDir.resolve(file.getStoredName()).normalize();
    }

    public Path getFilePathByStoredName(String storedName) {
        return uploadDir.resolve(storedName).normalize();
    }
}
