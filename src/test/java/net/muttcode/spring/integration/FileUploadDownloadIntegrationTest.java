package net.muttcode.spring.integration;

import net.muttcode.spring.config.JwtAuthenticationFilter;
import net.muttcode.spring.controller.FileController;
import net.muttcode.spring.service.CustomUserDetailsService;
import net.muttcode.spring.service.FileService;
import net.muttcode.spring.service.JwtService;
import net.muttcode.spring.service.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class FileUploadDownloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private Path testUploadDir;

    @BeforeEach
    void setUp() throws Exception {
        testUploadDir = Files.createTempDirectory("test-uploads");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testUploadDir != null && Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    @Test
    void uploadFile_shouldReturnFileIdAndFileName() throws Exception {
        // Arrange
        byte[] fileContent = "Test file content for upload".getBytes();
        MockMultipartFile testFile = new MockMultipartFile(
            "file",
            "test-image.png",
            "image/png",
            fileContent
        );

        StoredFile mockStoredFile = new StoredFile("test-file-id", "test-image.png", "test-file-id_test-image.png", testUploadDir);
        when(fileService.saveFile(any(MockMultipartFile.class))).thenReturn(mockStoredFile);

        // Act & Assert
        mockMvc.perform(multipart("/api/upload")
                .file(testFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileId").value("test-file-id"))
            .andExpect(jsonPath("$.fileName").value("test-file-id_test-image.png"));

        verify(fileService, times(1)).saveFile(any(MockMultipartFile.class));
    }

    @Test
    void downloadFile_shouldReturnFileWhenExists() throws Exception {
        // Arrange
        String fileId = "test-file-id-123";
        Path filePath = testUploadDir.resolve("test-file-id-123_test-image.png");
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "Test content".getBytes());

        when(fileService.getFilePath(fileId)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/api/upload/{fileId}", fileId))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"));

        verify(fileService, times(1)).getFilePath(fileId);
    }

    @Test
    void downloadFile_shouldReturn404ForNonExistentFile() throws Exception {
        // Arrange
        String nonExistentFileId = "00000000-0000-0000-0000-000000000000";
        when(fileService.getFilePath(nonExistentFileId))
            .thenThrow(new java.io.IOException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/api/upload/{fileId}", nonExistentFileId))
            .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_shouldReturn404WhenFileNotReadable() throws Exception {
        // Arrange
        String fileId = "test-file-id-456";
        Path filePath = testUploadDir.resolve("nonexistent.png");
        when(fileService.getFilePath(fileId)).thenReturn(filePath);

        // Act & Assert
        mockMvc.perform(get("/api/upload/{fileId}", fileId))
            .andExpect(status().isNotFound());
    }
}
