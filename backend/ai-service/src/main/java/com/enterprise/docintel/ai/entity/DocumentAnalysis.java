package com.enterprise.docintel.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_analysis")
public class DocumentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData;

    @Column(name = "possible_issues", columnDefinition = "TEXT")
    private String possibleIssues;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public DocumentAnalysis() {
    }

    public DocumentAnalysis(UUID id, UUID documentId, String summary, String extractedData, String possibleIssues, String extractedText, Double confidenceScore, LocalDateTime processedAt) {
        this.id = id;
        this.documentId = documentId;
        this.summary = summary;
        this.extractedData = extractedData;
        this.possibleIssues = possibleIssues;
        this.extractedText = extractedText;
        this.confidenceScore = confidenceScore;
        this.processedAt = processedAt;
    }

    @PrePersist
    protected void onProcess() {
        if (this.processedAt == null) {
            this.processedAt = LocalDateTime.now();
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

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
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

    public static DocumentAnalysisBuilder builder() {
        return new DocumentAnalysisBuilder();
    }

    public static class DocumentAnalysisBuilder {
        private UUID id;
        private UUID documentId;
        private String summary;
        private String extractedData;
        private String possibleIssues;
        private String extractedText;
        private Double confidenceScore;
        private LocalDateTime processedAt;

        DocumentAnalysisBuilder() {
        }

        public DocumentAnalysisBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public DocumentAnalysisBuilder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public DocumentAnalysisBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public DocumentAnalysisBuilder extractedData(String extractedData) {
            this.extractedData = extractedData;
            return this;
        }

        public DocumentAnalysisBuilder possibleIssues(String possibleIssues) {
            this.possibleIssues = possibleIssues;
            return this;
        }

        public DocumentAnalysisBuilder extractedText(String extractedText) {
            this.extractedText = extractedText;
            return this;
        }

        public DocumentAnalysisBuilder confidenceScore(Double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public DocumentAnalysisBuilder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public DocumentAnalysis build() {
            return new DocumentAnalysis(this.id, this.documentId, this.summary, this.extractedData, this.possibleIssues, this.extractedText, this.confidenceScore, this.processedAt);
        }
    }
}
