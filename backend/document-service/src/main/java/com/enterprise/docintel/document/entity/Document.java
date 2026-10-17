package com.enterprise.docintel.document.entity;

import com.enterprise.docintel.common.entity.DocumentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "owner_username", nullable = false)
    private String ownerUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Document() {
    }

    public Document(UUID id, String name, String filePath, String fileType, DocumentStatus status, UUID ownerId, String ownerUsername, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.fileType = fileType;
        this.status = status;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }

    public static class DocumentBuilder {
        private UUID id;
        private String name;
        private String filePath;
        private String fileType;
        private DocumentStatus status;
        private UUID ownerId;
        private String ownerUsername;
        private LocalDateTime createdAt;

        DocumentBuilder() {
        }

        public DocumentBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public DocumentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DocumentBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public DocumentBuilder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public DocumentBuilder status(DocumentStatus status) {
            this.status = status;
            return this;
        }

        public DocumentBuilder ownerId(UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public DocumentBuilder ownerUsername(String ownerUsername) {
            this.ownerUsername = ownerUsername;
            return this;
        }

        public DocumentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Document build() {
            return new Document(this.id, this.name, this.filePath, this.fileType, this.status, this.ownerId, this.ownerUsername, this.createdAt);
        }
    }
}
