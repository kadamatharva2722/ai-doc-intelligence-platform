package com.enterprise.docintel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CompareResponse {
    private String doc1Name;
    private String doc2Name;
    private String comparisonReport;
    private String differences;
}
