package net.muttcode.spring.service;

import net.muttcode.spring.model.File;
import net.muttcode.spring.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    private FileService fileService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        // Use default upload dir for tests
        fileService = new FileService(fileRepository, "./test-uploads");
    }

    @Test
    void saveFile_shouldSaveFileAndCreateEntity() throws IOException {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "test-image.png",
            "image/png",
            "test content".getBytes()
        );

        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            file.setId("mock-id-123");
            return file;
        });

        // Act
        StoredFile result = fileService.saveFile(mockFile);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getFileId());
        assertTrue(result.getStoredName().startsWith(result.getFileId() + "_"));
        assertEquals("test-image.png", result.getOriginalName());
        
        // Verify repository was called
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    void getFilePath_shouldReturnCorrectPathWhenFileExists() throws IOException {
        // Arrange
        String fileId = "test-file-id-123";
        String storedName = fileId + "_test-image.png";
        
        File mockFile = new File();
        ReflectionTestUtils.setField(mockFile, "fileId", fileId);
        ReflectionTestUtils.setField(mockFile, "storedName", storedName);

        when(fileRepository.findByFileId(fileId)).thenReturn(Optional.of(mockFile));

        // Act
        Path result = fileService.getFilePath(fileId);

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().endsWith(storedName));
        verify(fileRepository, times(1)).findByFileId(fileId);
    }

    @Test
    void getFilePath_shouldThrowExceptionWhenFileNotFound() {
        // Arrange
        String nonExistentFileId = "non-existent-file-id";
        when(fileRepository.findByFileId(nonExistentFileId)).thenReturn(Optional.empty());

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            fileService.getFilePath(nonExistentFileId);
        });
        
        assertTrue(exception.getMessage().contains("File not found"));
        verify(fileRepository, times(1)).findByFileId(nonExistentFileId);
    }

    @Test
    void getFilePathByStoredName_shouldReturnCorrectPath() throws IOException {
        // Arrange
        String storedName = "test-file-id_test-image.png";

        // Act
        Path result = fileService.getFilePathByStoredName(storedName);

        // Assert
        assertNotNull(result);
        assertTrue(result.toString().endsWith(storedName));
    }

    @Test
    void saveFile_shouldHandleFileWithNullContentType() throws IOException {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "test-file.txt",
            null,
            "test content".getBytes()
        );

        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(0);
            file.setId("mock-id-456");
            return file;
        });

        // Act
        StoredFile result = fileService.saveFile(mockFile);

        // Assert
        assertNotNull(result);
        verify(fileRepository, times(1)).save(any(File.class));
    }

    @Test
    void constructor_shouldUseCustomUploadDirWhenProvided() throws IOException {
        // Arrange
        String customUploadDir = "C:\\\\custom\\\\uploads";

        // Act
        FileService customFileService = new FileService(fileRepository, customUploadDir);
        Path uploadDir = (Path) ReflectionTestUtils.getField(customFileService, "uploadDir");

        // Assert
        assertNotNull(uploadDir);
        assertTrue(uploadDir.toAbsolutePath().toString().contains("custom"));
    }

    @Test
    void constructor_shouldUseDefaultUploadDirWhenNotProvided() throws IOException {
        // Arrange
        String defaultUploadDir = "./uploads";

        // Act
        FileService defaultFileService = new FileService(fileRepository, defaultUploadDir);
        Path uploadDir = (Path) ReflectionTestUtils.getField(defaultFileService, "uploadDir");

        // Assert
        assertNotNull(uploadDir);
        assertTrue(uploadDir.toAbsolutePath().toString().contains("uploads"));
    }
}
