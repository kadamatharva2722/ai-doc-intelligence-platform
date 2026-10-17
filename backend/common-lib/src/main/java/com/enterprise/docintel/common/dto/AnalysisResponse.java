package com.enterprise.docintel.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AnalysisResponse {
    private UUID id;
    private UUID documentId;
    private String summary;
    private String extractedData;
    private String possibleIssues;
    private Double confidenceScore;
    private LocalDateTime processedAt;

    public AnalysisResponse() {
    }

    public AnalysisResponse(UUID id, UUID documentId, String summary, String extractedData, String possibleIssues, Double confidenceScore, LocalDateTime processedAt) {
        this.id = id;
        this.documentId = documentId;
        this.summary = summary;
        this.extractedData = extractedData;
        this.possibleIssues = possibleIssues;
        this.confidenceScore = confidenceScore;
        this.processedAt = processedAt;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExtractedData() {
        return extractedData;
    }

    public void setExtractedData(String extractedData) {
        this.extractedData = extractedData;
    }

    public String getPossibleIssues() {
        return possibleIssues;
    }

    public void setPossibleIssues(String possibleIssues) {
        this.possibleIssues = possibleIssues;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public static AnalysisResponseBuilder builder() {
        return new AnalysisResponseBuilder();
    }

    public static class AnalysisResponseBuilder {
        private UUID id;
        private UUID documentId;
        private String summary;
        private String extractedData;
        private String possibleIssues;
        private Double confidenceScore;
        private LocalDateTime processedAt;

        AnalysisResponseBuilder() {
        }

        public AnalysisResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AnalysisResponseBuilder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public AnalysisResponseBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public AnalysisResponseBuilder extractedData(String extractedData) {
            this.extractedData = extractedData;
            return this;
        }

        public AnalysisResponseBuilder possibleIssues(String possibleIssues) {
            this.possibleIssues = possibleIssues;
            return this;
        }

        public AnalysisResponseBuilder confidenceScore(Double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public AnalysisResponseBuilder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public AnalysisResponse build() {
            return new AnalysisResponse(this.id, this.documentId, this.summary, this.extractedData, this.possibleIssues, this.confidenceScore, this.processedAt);
        }
    }
}
