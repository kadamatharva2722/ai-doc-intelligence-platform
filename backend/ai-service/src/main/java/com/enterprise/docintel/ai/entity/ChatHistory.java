package com.enterprise.docintel.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_history")
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChatHistory() {
    }

    public ChatHistory(UUID id, UUID documentId, UUID userId, String username, String question, String answer, LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.userId = userId;
        this.username = username;
        this.question = question;
        this.answer = answer;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static ChatHistoryBuilder builder() {
        return new ChatHistoryBuilder();
    }

    public static class ChatHistoryBuilder {
        private UUID id;
        private UUID documentId;
        private UUID userId;
        private String username;
        private String question;
        private String answer;
        private LocalDateTime createdAt;

        ChatHistoryBuilder() {
        }

        public ChatHistoryBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ChatHistoryBuilder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public ChatHistoryBuilder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public ChatHistoryBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ChatHistoryBuilder question(String question) {
            this.question = question;
            return this;
        }

        public ChatHistoryBuilder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public ChatHistoryBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ChatHistory build() {
            return new ChatHistory(this.id, this.documentId, this.userId, this.username, this.question, this.answer, this.createdAt);
        }
    }
}
