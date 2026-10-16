package com.enterprise.docintel.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AnalysisResponse {
    private UUID id;
    private UUID documentId;
    private String summary;
    private String extractedData;
    private String possibleIssues;
    private Double confidenceScore;
    private LocalDateTime processedAt;
}
