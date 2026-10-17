package com.enterprise.docintel.common.dto;

import java.util.UUID;

public class DocumentUploadedEvent {
    private UUID documentId;
    private String filename;
    private String filePath;
    private String fileType;
    private UUID ownerId;
    private String ownerUsername;

    public DocumentUploadedEvent() {
    }

    public DocumentUploadedEvent(UUID documentId, String filename, String filePath, String fileType, UUID ownerId, String ownerUsername) {
        this.documentId = documentId;
        this.filename = filename;
        this.filePath = filePath;
        this.fileType = fileType;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    // Alias for getFilename
    public String getName() {
        return filename;
    }

    // Alias for setFilename
    public void setName(String name) {
        this.filename = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public static DocumentUploadedEventBuilder builder() {
        return new DocumentUploadedEventBuilder();
    }

    public static class DocumentUploadedEventBuilder {
        private UUID documentId;
        private String filename;
        private String filePath;
        private String fileType;
        private UUID ownerId;
        private String ownerUsername;

        DocumentUploadedEventBuilder() {
        }

        public DocumentUploadedEventBuilder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public DocumentUploadedEventBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public DocumentUploadedEventBuilder name(String name) {
            this.filename = name;
            return this;
        }

        public DocumentUploadedEventBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public DocumentUploadedEventBuilder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public DocumentUploadedEventBuilder ownerId(UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public DocumentUploadedEventBuilder ownerUsername(String ownerUsername) {
            this.ownerUsername = ownerUsername;
            return this;
        }

        public DocumentUploadedEvent build() {
            return new DocumentUploadedEvent(this.documentId, this.filename, this.filePath, this.fileType, this.ownerId, this.ownerUsername);
        }
    }
}
