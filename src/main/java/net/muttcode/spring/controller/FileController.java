package net.muttcode.spring.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import net.muttcode.spring.service.FileService;

@RestController
@RequestMapping("/api")
public class FileController {

	@Autowired
	private FileService fileService;

	@PostMapping("/upload")
	public String uploadFile(@RequestParam("file") MultipartFile file) {
		try {
			String fileId = fileService.saveFile(file);
			System.out.println("Upload successful, fileId: " + fileId);
			return "{\"fileId\":\"" + fileId + "\"}";
		} catch (IOException e) {
			e.printStackTrace();
			return "{\"error\":\"" + e.getMessage() + "\"}";
		}
	}

	@GetMapping("/files/{fileId}")
	public Object downloadFile(@PathVariable String fileId) {
		return fileService.downloadFile(fileId);
	}
}
