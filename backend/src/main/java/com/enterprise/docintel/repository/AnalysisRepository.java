package com.enterprise.docintel.repository;

import com.enterprise.docintel.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<DocumentAnalysis, UUID> {
    Optional<DocumentAnalysis> findByDocumentId(UUID documentId);
}
