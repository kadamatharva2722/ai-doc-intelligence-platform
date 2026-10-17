package com.enterprise.docintel.ai.repository;

import com.enterprise.docintel.ai.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {
    List<ChatHistory> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);
    List<ChatHistory> findByUserId(UUID userId);
    List<ChatHistory> findByUsername(String username);
}
