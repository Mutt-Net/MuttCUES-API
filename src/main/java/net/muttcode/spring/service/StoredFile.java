package net.muttcode.spring.service;

import java.nio.file.Path;

public class StoredFile {

    private final String fileId;
    private final String originalName;
    private final String storedName;
    private final Path path;

    public StoredFile(String fileId, String originalName, String storedName, Path path) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.path = path;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public Path getPath() {
        return path;
    }
}
