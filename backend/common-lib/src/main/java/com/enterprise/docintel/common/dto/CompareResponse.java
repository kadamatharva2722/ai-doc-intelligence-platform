package com.enterprise.docintel.common.dto;

import java.util.UUID;

public class CompareResponse {
    private String doc1Name;
    private String doc2Name;
    private String comparisonReport;
    private String differences;
    private UUID documentId1;
    private UUID documentId2;

    public CompareResponse() {
    }

    public CompareResponse(String doc1Name, String doc2Name, String comparisonReport, String differences, UUID documentId1, UUID documentId2) {
        this.doc1Name = doc1Name;
        this.doc2Name = doc2Name;
        this.comparisonReport = comparisonReport;
        this.differences = differences;
        this.documentId1 = documentId1;
        this.documentId2 = documentId2;
    }

    public String getDoc1Name() {
        return doc1Name;
    }

    public void setDoc1Name(String doc1Name) {
        this.doc1Name = doc1Name;
    }

    public String getDoc2Name() {
        return doc2Name;
    }

    public void setDoc2Name(String doc2Name) {
        this.doc2Name = doc2Name;
    }

    public String getComparisonReport() {
        return comparisonReport;
    }

    public void setComparisonReport(String comparisonReport) {
        this.comparisonReport = comparisonReport;
    }

    public String getDifferences() {
        return differences;
    }

    public void setDifferences(String differences) {
        this.differences = differences;
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

    public static CompareResponseBuilder builder() {
        return new CompareResponseBuilder();
    }

    public static class CompareResponseBuilder {
        private String doc1Name;
        private String doc2Name;
        private String comparisonReport;
        private String differences;
        private UUID documentId1;
        private UUID documentId2;

        CompareResponseBuilder() {
        }

        public CompareResponseBuilder doc1Name(String doc1Name) {
            this.doc1Name = doc1Name;
            return this;
        }

        public CompareResponseBuilder doc2Name(String doc2Name) {
            this.doc2Name = doc2Name;
            return this;
        }

        public CompareResponseBuilder comparisonReport(String comparisonReport) {
            this.comparisonReport = comparisonReport;
            return this;
        }

        public CompareResponseBuilder differences(String differences) {
            this.differences = differences;
            return this;
        }

        public CompareResponseBuilder documentId1(UUID documentId1) {
            this.documentId1 = documentId1;
            return this;
        }

        public CompareResponseBuilder documentId2(UUID documentId2) {
            this.documentId2 = documentId2;
            return this;
        }

        public CompareResponse build() {
            return new CompareResponse(this.doc1Name, this.doc2Name, this.comparisonReport, this.differences, this.documentId1, this.documentId2);
        }
    }
}
