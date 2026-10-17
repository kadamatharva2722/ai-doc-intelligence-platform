package com.enterprise.docintel.common.dto;

import com.enterprise.docintel.common.entity.DocumentStatus;
import java.util.UUID;

public class DocumentStatusUpdatedEvent {
    private UUID documentId;
    private DocumentStatus status;
    private String errorMessage;

    public DocumentStatusUpdatedEvent() {
    }

    public DocumentStatusUpdatedEvent(UUID documentId, DocumentStatus status, String errorMessage) {
        this.documentId = documentId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static DocumentStatusUpdatedEventBuilder builder() {
        return new DocumentStatusUpdatedEventBuilder();
    }

    public static class DocumentStatusUpdatedEventBuilder {
        private UUID documentId;
        private DocumentStatus status;
        private String errorMessage;

        DocumentStatusUpdatedEventBuilder() {
        }

        public DocumentStatusUpdatedEventBuilder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public DocumentStatusUpdatedEventBuilder status(DocumentStatus status) {
            this.status = status;
            return this;
        }

        public DocumentStatusUpdatedEventBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public DocumentStatusUpdatedEvent build() {
            return new DocumentStatusUpdatedEvent(this.documentId, this.status, this.errorMessage);
        }
    }
}
