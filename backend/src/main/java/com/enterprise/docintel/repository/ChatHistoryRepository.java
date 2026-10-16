package com.enterprise.docintel.repository;

import com.enterprise.docintel.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {
    List<ChatHistory> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
    List<ChatHistory> findByUserId(UUID userId);
}
