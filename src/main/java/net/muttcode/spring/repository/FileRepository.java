package net.muttcode.spring.repository;

import net.muttcode.spring.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    Optional<File> findByFileId(String fileId);
    List<File> findByStatusOrderByUploadDateDesc(File.FileStatus status);
    List<File> findAllByOrderByUploadDateDesc();
}