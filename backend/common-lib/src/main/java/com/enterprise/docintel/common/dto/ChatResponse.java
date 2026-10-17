package com.enterprise.docintel.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatResponse {
    private UUID id;
    private UUID documentId;
    private String question;
    private String answer;
    private String username;
    private LocalDateTime createdAt;

    public ChatResponse() {
    }

    public ChatResponse(String question, String answer, LocalDateTime createdAt) {
        this.question = question;
        this.answer = answer;
        this.createdAt = createdAt;
    }

    public ChatResponse(UUID id, UUID documentId, String question, String answer, String username, LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.question = question;
        this.answer = answer;
        this.username = username;
        this.createdAt = createdAt;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static ChatResponseBuilder builder() {
        return new ChatResponseBuilder();
    }

    public static class ChatResponseBuilder {
        private UUID id;
        private UUID documentId;
        private String question;
        private String answer;
        private String username;
        private LocalDateTime createdAt;

        ChatResponseBuilder() {
        }

        public ChatResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ChatResponseBuilder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public ChatResponseBuilder question(String question) {
            this.question = question;
            return this;
        }

        public ChatResponseBuilder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public ChatResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ChatResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(this.id, this.documentId, this.question, this.answer, this.username, this.createdAt);
        }
    }
}
