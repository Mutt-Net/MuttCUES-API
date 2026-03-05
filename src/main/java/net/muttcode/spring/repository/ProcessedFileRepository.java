package net.muttcode.spring.repository;

import net.muttcode.spring.model.ProcessedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedFileRepository extends JpaRepository<ProcessedFile, String> {
    Optional<ProcessedFile> findByFileId(String fileId);
    Optional<ProcessedFile> findByProcessedFileId(String processedFileId);
    List<ProcessedFile> findByStatusOrderByCreatedAtDesc(ProcessedFile.ProcessedFileStatus status);
    List<ProcessedFile> findAllByOrderByCreatedAtDesc();
}
