package net.muttcode.spring.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileService {

    private final Path uploadDir;

    public FileService() throws IOException {
        this.uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        Files.createDirectories(uploadDir);
        System.out.println("Upload dir: " + uploadDir.toAbsolutePath());
    }

    public StoredFile saveFile(MultipartFile file) throws IOException {

        String fileId = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename();
        String storedName = fileId + "_" + originalName;

        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(fileId, originalName, storedName, target);
    }

    public Path getFilePath1(String storedName) {
        return uploadDir.resolve(storedName).normalize();
    }
    public Path getFilePath(String fileId) {
    return uploadDir.resolve(fileId).normalize();
}

}
