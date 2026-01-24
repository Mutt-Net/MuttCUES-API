package net.muttcode.spring.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

	private final Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");

	public FileService() throws IOException {
		if (!Files.exists(uploadDir)) {
			Files.createDirectories(uploadDir);
			System.out.println("Upload directory created at: " + uploadDir.toAbsolutePath());
		} else {
			System.out.println("Using existing upload directory: " + uploadDir.toAbsolutePath());
		}
	}

	public String saveFile(MultipartFile file) throws IOException {
		String fileId = UUID.randomUUID().toString();
		Path filePath = uploadDir.resolve(fileId);
		System.out.println("Attempting to save file: " + file.getOriginalFilename() + " -> " + filePath);
		file.transferTo(filePath.toFile());
		System.out.println("File saved successfully: " + filePath.toAbsolutePath());
		return fileId;
	}

	public ResponseEntity<FileSystemResource> downloadFile(String fileId) {
		Path filePath = uploadDir.resolve(fileId);
		File file = filePath.toFile();

		if (!file.exists()) {
			System.out.println("Download failed: file not found: " + filePath.toAbsolutePath());
			return ResponseEntity.notFound().build();
		}

		System.out.println("Downloading file: " + filePath.toAbsolutePath());
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + fileId + "\"")
				.body(new FileSystemResource(file));
	}
}
