package net.muttcode.spring.model;

import java.nio.file.Path;

public class ProcessedFile {
    private String fileId;
    private String fileName;
    private Path filePath;
    private String contentType;
    private long fileSize;
    
    public ProcessedFile() {}
    
    public ProcessedFile(String fileId, String fileName, Path filePath, String contentType) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.contentType = contentType;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Path getFilePath() { return filePath; }
    public void setFilePath(Path filePath) { this.filePath = filePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
