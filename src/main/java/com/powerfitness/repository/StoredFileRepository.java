package com.powerfitness.repository;

import com.powerfitness.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByFileKey(String fileKey);
    Optional<StoredFile> findByCategoryAndFileKey(String category, String fileKey);
    boolean existsByFileKey(String fileKey);
    void deleteByFileKey(String fileKey);
}
