package com.enterprise.docintel.common.dto;

import java.util.UUID;

public class CompareRequest {

    private UUID documentId1;
    private UUID documentId2;

    public CompareRequest() {
    }

    public CompareRequest(UUID documentId1, UUID documentId2) {
        this.documentId1 = documentId1;
        this.documentId2 = documentId2;
    }

    public UUID getDocumentId1() {
        return documentId1;
    }

    public void setDocumentId1(UUID documentId1) {
        this.documentId1 = documentId1;
    }

    public UUID getDocumentId2() {
        return documentId2;
    }

    public void setDocumentId2(UUID documentId2) {
        this.documentId2 = documentId2;
    }

    // Aliases for document1Id / document2Id
    public UUID getDocument1Id() {
        return documentId1;
    }

    public void setDocument1Id(UUID document1Id) {
        this.documentId1 = document1Id;
    }

    public UUID getDocument2Id() {
        return documentId2;
    }

    public void setDocument2Id(UUID document2Id) {
        this.documentId2 = document2Id;
    }

    public static CompareRequestBuilder builder() {
        return new CompareRequestBuilder();
    }

    public static class CompareRequestBuilder {
        private UUID documentId1;
        private UUID documentId2;

        CompareRequestBuilder() {
        }

        public CompareRequestBuilder documentId1(UUID documentId1) {
            this.documentId1 = documentId1;
            return this;
        }

        public CompareRequestBuilder documentId2(UUID documentId2) {
            this.documentId2 = documentId2;
            return this;
        }

        public CompareRequestBuilder document1Id(UUID document1Id) {
            this.documentId1 = document1Id;
            return this;
        }

        public CompareRequestBuilder document2Id(UUID document2Id) {
            this.documentId2 = document2Id;
            return this;
        }

        public CompareRequest build() {
            return new CompareRequest(this.documentId1, this.documentId2);
        }
    }
}
