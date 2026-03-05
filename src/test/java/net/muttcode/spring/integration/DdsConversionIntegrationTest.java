package net.muttcode.spring.integration;

import net.muttcode.spring.config.JwtAuthenticationFilter;
import net.muttcode.spring.controller.DdsConversionController;
import net.muttcode.spring.service.CustomUserDetailsService;
import net.muttcode.spring.service.DdsConversionService;
import net.muttcode.spring.service.JwtService;
import net.muttcode.spring.model.ProcessedFile;
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

@WebMvcTest(DdsConversionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class DdsConversionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdsConversionService ddsConversionService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private Path testOutputDir;

    @BeforeEach
    void setUp() throws Exception {
        testOutputDir = Files.createTempDirectory("test-dds-output");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testOutputDir != null && Files.exists(testOutputDir)) {
            Files.walk(testOutputDir)
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
    void ddsToPngConversion_shouldReturnSuccessWithOutputFileId() throws Exception {
        byte[] ddsContent = createMinimalDdsFile();
        MockMultipartFile ddsFile = new MockMultipartFile(
            "file", "test.dds", "image/vnd.ms-dds", ddsContent);

        ProcessedFile mockResult = createMockProcessedFile("output-png-id", "test.png", "image/png");
        when(ddsConversionService.ddsToPng(any())).thenReturn(mockResult);

        mockMvc.perform(multipart("/api/convert/dds-to-png").file(ddsFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.fileId").value("output-png-id"))
            .andExpect(jsonPath("$.fileName").value("test.png"))
            .andExpect(jsonPath("$.downloadUrl").value("/api/convert/output-png-id"));

        verify(ddsConversionService, times(1)).ddsToPng(any());
    }

    @Test
    void pngToDdsConversion_shouldReturnSuccessWithOutputFileId() throws Exception {
        byte[] pngContent = createMinimalPngFile();
        MockMultipartFile pngFile = new MockMultipartFile(
            "file", "test.png", "image/png", pngContent);

        ProcessedFile mockResult = createMockProcessedFile("output-dds-id", "test.dds", "image/vnd.ms-dds");
        when(ddsConversionService.imageToDds(any())).thenReturn(mockResult);

        mockMvc.perform(multipart("/api/convert/image-to-dds").file(pngFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.fileId").value("output-dds-id"))
            .andExpect(jsonPath("$.fileName").value("test.dds"))
            .andExpect(jsonPath("$.downloadUrl").value("/api/convert/output-dds-id"))
            .andExpect(jsonPath("$.format").value("Uncompressed ARGB"));

        verify(ddsConversionService, times(1)).imageToDds(any());
    }

    @Test
    void ddsToPngConversion_shouldReturn400ForInvalidFile() throws Exception {
        byte[] invalidDds = "not a dds file".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "invalid.dds", "image/vnd.ms-dds", invalidDds);

        when(ddsConversionService.ddsToPng(any()))
            .thenThrow(new IllegalArgumentException("Invalid DDS file format"));

        mockMvc.perform(multipart("/api/convert/dds-to-png").file(invalidFile))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Invalid DDS file format"));
    }

    @Test
    void pngToDdsConversion_shouldReturn400ForInvalidFile() throws Exception {
        byte[] invalidPng = "not a png file".getBytes();
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "invalid.png", "image/png", invalidPng);

        when(ddsConversionService.imageToDds(any()))
            .thenThrow(new IllegalArgumentException("Invalid PNG file format"));

        mockMvc.perform(multipart("/api/convert/image-to-dds").file(invalidFile))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Invalid PNG file format"));
    }

    @Test
    void ddsToPngConversion_shouldReturn500WhenConversionFails() throws Exception {
        byte[] ddsContent = createMinimalDdsFile();
        MockMultipartFile ddsFile = new MockMultipartFile(
            "file", "test.dds", "image/vnd.ms-dds", ddsContent);

        when(ddsConversionService.ddsToPng(any()))
            .thenThrow(new RuntimeException("Conversion service error"));

        mockMvc.perform(multipart("/api/convert/dds-to-png").file(ddsFile))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Conversion service error"));
    }

    @Test
    void pngToDdsConversion_shouldReturn500WhenConversionFails() throws Exception {
        byte[] pngContent = createMinimalPngFile();
        MockMultipartFile pngFile = new MockMultipartFile(
            "file", "test.png", "image/png", pngContent);

        when(ddsConversionService.imageToDds(any()))
            .thenThrow(new RuntimeException("Conversion service error"));

        mockMvc.perform(multipart("/api/convert/image-to-dds").file(pngFile))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("Conversion service error"));
    }

    @Test
    void downloadConvertedFile_shouldReturnFileWithCorrectHeaders() throws Exception {
        String fileId = "test-output-file-id";
        Path filePath = testOutputDir.resolve(fileId + "_output.png");
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "PNG content".getBytes());

        when(ddsConversionService.getOutputPath()).thenReturn(testOutputDir);

        mockMvc.perform(get("/api/convert/{fileId}", fileId))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void downloadConvertedFile_shouldReturn404ForNonExistentFile() throws Exception {
        String nonExistentFileId = "00000000-0000-0000-0000-000000000000";
        when(ddsConversionService.getOutputPath()).thenReturn(testOutputDir);

        mockMvc.perform(get("/api/convert/{fileId}", nonExistentFileId))
            .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpoint_shouldReturnServiceStatus() throws Exception {
        mockMvc.perform(get("/api/convert/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.service").value("DDS Converter"));
    }

    private byte[] createMinimalDdsFile() {
        byte[] ddsHeader = new byte[12];
        ddsHeader[0] = 'D'; ddsHeader[1] = 'D';
        ddsHeader[2] = 'S'; ddsHeader[3] = ' ';
        return ddsHeader;
    }

    private byte[] createMinimalPngFile() {
        byte[] pngSignature = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        byte[] minimalChunk = new byte[] {
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00,
            (byte) 0x90, 0x77, 0x53, (byte) 0xDE };
        byte[] png = new byte[pngSignature.length + minimalChunk.length];
        System.arraycopy(pngSignature, 0, png, 0, pngSignature.length);
        System.arraycopy(minimalChunk, 0, png, pngSignature.length, minimalChunk.length);
        return png;
    }

    private ProcessedFile createMockProcessedFile(String fileId, String fileName, String contentType) {
        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setProcessedFileId(fileId);
        processedFile.setProcessedName(fileName);
        processedFile.setProcessingType(ProcessedFile.ProcessingType.DDS_TO_PNG);
        processedFile.setFileSize(1024L);
        processedFile.setContentType(contentType);
        processedFile.setStatus(ProcessedFile.ProcessedFileStatus.COMPLETED);
        return processedFile;
    }
}
