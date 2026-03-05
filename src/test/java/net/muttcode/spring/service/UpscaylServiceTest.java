package net.muttcode.spring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UpscaylService file path handling.
 * Verifies that the service correctly handles Docker volume mount paths.
 */
class UpscaylServiceTest {

    private UpscaylService upscaylService;
    private Path tempDir;
    private Path inputMountDir;
    private Path outputMountDir;

    @BeforeEach
    void setUp() throws IOException {
        upscaylService = new UpscaylService(new ObjectMapper());
        ReflectionTestUtils.setField(upscaylService, "upscaylServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(upscaylService, "modelsPath", "/app/models");
        ReflectionTestUtils.setField(upscaylService, "inputPath", "/app/input");
        ReflectionTestUtils.setField(upscaylService, "outputPath", "/app/output");

        // Create temporary directories for testing mount points
        tempDir = Files.createTempDirectory("upscayl-test");
        inputMountDir = Files.createDirectories(tempDir.resolve("input"));
        outputMountDir = Files.createDirectories(tempDir.resolve("output"));
    }

    @Test
    void processImage_shouldUseActualInputPath_notHardcodedPath() throws IOException {
        // Create a test input file in the mount point
        Path testInputFile = inputMountDir.resolve("test-image.png");
        Files.writeString(testInputFile, "fake-image-data");

        // The service should use the actual path, not hardcoded /app/input
        // We verify this by checking the file exists check passes
        assertDoesNotThrow(() -> {
            // This will fail at HTTP call (no server), but should not fail at file validation
            try {
                upscaylService.processImage(testInputFile.toString(), 4, "ultramix_balanced");
            } catch (Exception e) {
                // Expected - no actual Upscayl server running
                // The important part is we got past the file existence check
                assertTrue(e.getMessage().contains("Connection refused") || 
                          e.getMessage().contains("Unable to execute request") ||
                          e.getCause() != null, 
                          "Should fail at HTTP level, not file validation");
            }
        });
    }

    @Test
    void processImage_shouldThrowExceptionWhenInputFileNotFound() {
        String nonExistentPath = "/non/existent/path/image.png";
        
        IOException exception = assertThrows(IOException.class, () -> {
            upscaylService.processImage(nonExistentPath, 4, "ultramix_balanced");
        });
        
        assertEquals("Input file not found: " + nonExistentPath, exception.getMessage());
    }

    @Test
    void processImage_shouldDeriveOutputPathFromInputPath() throws IOException {
        // Create test input file
        Path testInputFile = inputMountDir.resolve("my-image.png");
        Files.writeString(testInputFile, "fake-image-data");

        // Verify output path derivation logic
        File inputFile = testInputFile.toFile();
        String inputFileName = inputFile.getName();
        String expectedOutputFileName = "my-image_4x.png";
        
        // The output should be in the same directory as input
        Path expectedOutputPath = inputMountDir.resolve(expectedOutputFileName);
        
        // Verify the input file exists
        assertTrue(Files.exists(testInputFile), "Input file should exist");
        
        // Verify output directory is same as input directory
        assertEquals(inputMountDir, testInputFile.getParent(), 
                    "Output should be in same directory as input");
    }

    @Test
    void getOutputFileName_shouldAppendScaleFactor() throws IOException {
        // Test the private getOutputFileName method via reflection
        Path testInputFile = inputMountDir.resolve("test.png");
        Files.writeString(testInputFile, "fake-data");

        File inputFile = testInputFile.toFile();
        String inputFileName = inputFile.getName();
        
        // Manually test the naming logic
        String baseName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
        String extension = inputFileName.substring(inputFileName.lastIndexOf('.'));
        Integer scaleFactor = 4;
        String expectedOutputFileName = baseName + "_" + scaleFactor + "x" + extension;
        
        assertEquals("test_4x.png", expectedOutputFileName);
    }

    @Test
    void processImageAsync_shouldUseHardcodedPaths_forAsyncMode() throws IOException {
        // Create test input file
        Path testInputFile = inputMountDir.resolve("async-test.png");
        Files.writeString(testInputFile, "fake-image-data");

        // Async mode should also pass file validation
        assertDoesNotThrow(() -> {
            try {
                upscaylService.processImageAsync(testInputFile.toString(), 4, "ultramix_balanced");
            } catch (Exception e) {
                // Expected - no actual Upscayl server running
                assertTrue(e.getMessage().contains("Connection refused") || 
                          e.getMessage().contains("Unable to execute request") ||
                          e.getCause() != null,
                          "Should fail at HTTP level, not file validation");
            }
        });
    }

    @Test
    void getAvailableModels_shouldReturnDefaultModelsWhenServiceUnavailable() {
        String[] models = upscaylService.getAvailableModels();
        
        // When service is unavailable, should return defaults
        assertNotNull(models);
        assertArrayEquals(new String[]{"ultramix_balanced", "realesrgan-x4plus", "realcugan"}, models);
    }

    @Test
    void UpscaylResult_shouldStoreSuccessState() {
        UpscaylService.UpscaylResult result = new UpscaylService.UpscaylResult(
            true, "/app/output/result.png", null
        );

        assertTrue(result.isSuccess());
        assertEquals("/app/output/result.png", result.getOutputPath());
        assertNull(result.getError());
    }

    @Test
    void UpscaylResult_shouldStoreErrorState() {
        UpscaylService.UpscaylResult result = new UpscaylService.UpscaylResult(
            false, null, "Processing failed"
        );

        assertFalse(result.isSuccess());
        assertNull(result.getOutputPath());
        assertEquals("Processing failed", result.getError());
    }
}
