package net.muttcode.spring.repository;

import net.muttcode.spring.model.File;
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
class FileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FileRepository fileRepository;

    private File testFile;

    @BeforeEach
    void setUp() {
        testFile = new File(
            "test-file-id-123",
            "test-image.png",
            "test-file-id-123_test-image.png",
            1024L,
            "image/png"
        );
        entityManager.persistAndFlush(testFile);
    }

    @Test
    void findByFileId_shouldReturnFile() {
        Optional<File> found = fileRepository.findByFileId("test-file-id-123");

        assertTrue(found.isPresent());
        assertEquals("test-image.png", found.get().getOriginalName());
        assertEquals("test-file-id-123_test-image.png", found.get().getStoredName());
        assertEquals(1024L, found.get().getFileSize());
        assertEquals("image/png", found.get().getContentType());
    }

    @Test
    void findByFileId_shouldReturnEmptyForNonExistentFile() {
        Optional<File> found = fileRepository.findByFileId("non-existent-id");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByStatus_shouldReturnFilesWithStatus() {
        File anotherFile = new File(
            "another-file-id",
            "another.png",
            "another-file-id_another.png",
            2048L,
            "image/png"
        );
        anotherFile.setStatus(File.FileStatus.PROCESSING);
        entityManager.persistAndFlush(anotherFile);

        List<File> uploadedFiles = fileRepository.findByStatusOrderByUploadDateDesc(File.FileStatus.UPLOADED);
        List<File> processingFiles = fileRepository.findByStatusOrderByUploadDateDesc(File.FileStatus.PROCESSING);

        assertEquals(1, uploadedFiles.size());
        assertEquals("test-file-id-123", uploadedFiles.get(0).getFileId());
        assertEquals(1, processingFiles.size());
        assertEquals("another-file-id", processingFiles.get(0).getFileId());
    }

    @Test
    void findAllByOrderByUploadDateDesc_shouldReturnAllFiles() {
        File anotherFile = new File(
            "another-file-id",
            "another.png",
            "another-file-id_another.png",
            2048L,
            "image/png"
        );
        entityManager.persistAndFlush(anotherFile);

        List<File> allFiles = fileRepository.findAllByOrderByUploadDateDesc();

        assertEquals(2, allFiles.size());
    }

    @Test
    void fileEntity_shouldPersistAndRetrieveCorrectly() {
        File.FileStatus status = testFile.getStatus();

        assertNotNull(testFile.getId());
        assertNotNull(testFile.getUploadDate());
        assertEquals(File.FileStatus.UPLOADED, status);
    }

    @Test
    void fileEntity_setterMethodsShouldWork() {
        testFile.setOriginalName("modified.png");
        testFile.setStoredName("test-file-id-123_modified.png");
        testFile.setFileSize(4096L);
        testFile.setContentType("image/png");
        testFile.setStatus(File.FileStatus.PROCESSING);
        testFile.setUploadedBy("testuser");
        testFile.setMetadata("{\"key\": \"value\"}");

        entityManager.flush();

        assertEquals("modified.png", testFile.getOriginalName());
        assertEquals(4096L, testFile.getFileSize());
        assertEquals(File.FileStatus.PROCESSING, testFile.getStatus());
        assertEquals("testuser", testFile.getUploadedBy());
        assertEquals("{\"key\": \"value\"}", testFile.getMetadata());
    }
}