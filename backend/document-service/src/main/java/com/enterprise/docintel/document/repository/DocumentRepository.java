package com.enterprise.docintel.document.repository;

import com.enterprise.docintel.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    List<Document> findByOwnerUsername(String ownerUsername);
    List<Document> findByOwnerId(UUID ownerId);
    long countByOwnerUsername(String ownerUsername);
    long countByOwnerId(UUID ownerId);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long countByOwnerUsernameAndCreatedAtAfter(String ownerUsername, LocalDateTime dateTime);
}
