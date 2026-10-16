package com.enterprise.docintel.repository;

import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    List<Document> findByOwner(User owner);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
