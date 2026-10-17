package com.enterprise.docintel.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentResponse {
    private UUID id;
    private String name;
    private String fileType;
    private String status;
    private String ownerUsername;
    private LocalDateTime createdAt;

    public DocumentResponse() {
    }

    public DocumentResponse(UUID id, String name, String fileType, String status, String ownerUsername, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.fileType = fileType;
        this.status = status;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static DocumentResponseBuilder builder() {
        return new DocumentResponseBuilder();
    }

    public static class DocumentResponseBuilder {
        private UUID id;
        private String name;
        private String fileType;
        private String status;
        private String ownerUsername;
        private LocalDateTime createdAt;

        DocumentResponseBuilder() {
        }

        public DocumentResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public DocumentResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DocumentResponseBuilder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public DocumentResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public DocumentResponseBuilder ownerUsername(String ownerUsername) {
            this.ownerUsername = ownerUsername;
            return this;
        }

        public DocumentResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DocumentResponse build() {
            return new DocumentResponse(this.id, this.name, this.fileType, this.status, this.ownerUsername, this.createdAt);
        }
    }
}
