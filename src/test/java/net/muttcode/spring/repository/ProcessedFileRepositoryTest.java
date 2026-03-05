package net.muttcode.spring.repository;

import net.muttcode.spring.model.File;
import net.muttcode.spring.model.ProcessedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ProcessedFileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProcessedFileRepository processedFileRepository;

    private File testFile;
    private ProcessedFile testProcessedFile;

    @BeforeEach
    void setUp() {
        // Create and persist a File entity first (required for foreign key)
        testFile = new File(
            "test-file-id-123",
            "test-image.png",
            "test-file-id-123_test-image.png",
            1024L,
            "image/png"
        );
        entityManager.persistAndFlush(testFile);

        // Create and persist a ProcessedFile entity
        testProcessedFile = new ProcessedFile(
            testFile,
            "processed-file-id-123",
            "test-image-upscaled.png",
            ProcessedFile.ProcessingType.UPSCALE
        );
        entityManager.persistAndFlush(testProcessedFile);
    }

    @Test
    void findByFileId_shouldReturnProcessedFile() {
        Optional<ProcessedFile> found = processedFileRepository.findByFileId(testFile.getId());

        assertTrue(found.isPresent());
        assertEquals("test-image-upscaled.png", found.get().getProcessedName());
        assertEquals(ProcessedFile.ProcessingType.UPSCALE, found.get().getProcessingType());
        assertEquals(ProcessedFile.ProcessedFileStatus.PROCESSING, found.get().getStatus());
    }

    @Test
    void findByFileId_shouldReturnEmptyForNonExistentFile() {
        String nonExistentFileId = "non-existent-file-id";

        Optional<ProcessedFile> found = processedFileRepository.findByFileId(nonExistentFileId);

        assertTrue(found.isEmpty());
    }

    @Test
    void findByProcessedFileId_shouldReturnProcessedFile() {
        Optional<ProcessedFile> found = processedFileRepository.findByProcessedFileId("processed-file-id-123");

        assertTrue(found.isPresent());
        assertEquals("test-image-upscaled.png", found.get().getProcessedName());
        assertEquals(testFile.getId(), found.get().getFile().getId());
    }

    @Test
    void findByProcessedFileId_shouldReturnEmptyForNonExistentId() {
        Optional<ProcessedFile> found = processedFileRepository.findByProcessedFileId("non-existent-processed-id");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByStatus_shouldReturnFilesWithStatus() {
        // Create another processed file with COMPLETED status
        File anotherFile = new File(
            "another-file-id",
            "another.png",
            "another-file-id_another.png",
            2048L,
            "image/png"
        );
        entityManager.persistAndFlush(anotherFile);

        ProcessedFile completedProcessedFile = new ProcessedFile(
            anotherFile,
            "processed-file-id-456",
            "another-upscaled.png",
            ProcessedFile.ProcessingType.UPSCALE
        );
        completedProcessedFile.setStatus(ProcessedFile.ProcessedFileStatus.COMPLETED);
        entityManager.persistAndFlush(completedProcessedFile);

        List<ProcessedFile> processingFiles = processedFileRepository.findByStatusOrderByCreatedAtDesc(ProcessedFile.ProcessedFileStatus.PROCESSING);
        List<ProcessedFile> completedFiles = processedFileRepository.findByStatusOrderByCreatedAtDesc(ProcessedFile.ProcessedFileStatus.COMPLETED);

        assertEquals(1, processingFiles.size());
        assertEquals("test-image-upscaled.png", processingFiles.get(0).getProcessedName());
        assertEquals(1, completedFiles.size());
        assertEquals("another-upscaled.png", completedFiles.get(0).getProcessedName());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_shouldReturnAllProcessedFiles() {
        // Create another processed file
        File anotherFile = new File(
            "another-file-id",
            "another.png",
            "another-file-id_another.png",
            2048L,
            "image/png"
        );
        entityManager.persistAndFlush(anotherFile);

        ProcessedFile anotherProcessedFile = new ProcessedFile(
            anotherFile,
            "processed-file-id-456",
            "another-upscaled.png",
            ProcessedFile.ProcessingType.UPSCALE
        );
        entityManager.persistAndFlush(anotherProcessedFile);

        List<ProcessedFile> allProcessedFiles = processedFileRepository.findAllByOrderByCreatedAtDesc();

        assertEquals(2, allProcessedFiles.size());
    }

    @Test
    void processedFileEntity_shouldPersistAndRetrieveCorrectly() {
        assertNotNull(testProcessedFile.getId());
        assertNotNull(testProcessedFile.getCreatedAt());
        assertNotNull(testProcessedFile.getUpdatedAt());
        assertEquals(ProcessedFile.ProcessedFileStatus.PROCESSING, testProcessedFile.getStatus());
        assertEquals(testFile.getId(), testProcessedFile.getFile().getId());
    }

    @Test
    void processedFileEntity_setterMethodsShouldWork() {
        testProcessedFile.setProcessedName("modified-upscaled.png");
        testProcessedFile.setFileSize(4096L);
        testProcessedFile.setContentType("image/png");
        testProcessedFile.setStatus(ProcessedFile.ProcessedFileStatus.COMPLETED);
        testProcessedFile.setErrorMessage(null);
        testProcessedFile.setProcessingStartedAt(java.time.Instant.now());
        testProcessedFile.setProcessingCompletedAt(java.time.Instant.now());
        testProcessedFile.setProcessingParams("{\"model\": \"realesrgan\"}");

        entityManager.flush();

        assertEquals("modified-upscaled.png", testProcessedFile.getProcessedName());
        assertEquals(4096L, testProcessedFile.getFileSize());
        assertEquals(ProcessedFile.ProcessedFileStatus.COMPLETED, testProcessedFile.getStatus());
        assertNotNull(testProcessedFile.getProcessingCompletedAt());
        assertEquals("{\"model\": \"realesrgan\"}", testProcessedFile.getProcessingParams());
    }

    @Test
    void processedFileEntity_differentProcessingTypesShouldWork() {
        File ddsFile = new File(
            "dds-file-id",
            "texture.dds",
            "dds-file-id_texture.dds",
            4096L,
            "image/dds"
        );
        entityManager.persistAndFlush(ddsFile);

        ProcessedFile pngFromDds = new ProcessedFile(
            ddsFile,
            "png-from-dds-id",
            "texture.png",
            ProcessedFile.ProcessingType.DDS_TO_PNG
        );
        entityManager.persistAndFlush(pngFromDds);

        ProcessedFile ddsFromPng = new ProcessedFile(
            testFile,
            "dds-from-png-id",
            "test-image.dds",
            ProcessedFile.ProcessingType.IMAGE_TO_DDS
        );
        entityManager.persistAndFlush(ddsFromPng);

        List<ProcessedFile> allFiles = processedFileRepository.findAllByOrderByCreatedAtDesc();

        assertEquals(3, allFiles.size());
        assertTrue(allFiles.stream().anyMatch(pf -> pf.getProcessingType() == ProcessedFile.ProcessingType.DDS_TO_PNG));
        assertTrue(allFiles.stream().anyMatch(pf -> pf.getProcessingType() == ProcessedFile.ProcessingType.IMAGE_TO_DDS));
        assertTrue(allFiles.stream().anyMatch(pf -> pf.getProcessingType() == ProcessedFile.ProcessingType.UPSCALE));
    }
}
