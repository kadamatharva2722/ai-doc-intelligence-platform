package com.enterprise.docintel.ai.service;

import com.enterprise.docintel.common.dto.*;
import java.util.List;
import java.util.UUID;

public interface AnalysisService {
    AnalysisResponse getAnalysisByDocumentId(UUID documentId, String currentUser, String currentRole);
    ChatResponse askQuestion(UUID documentId, ChatRequest request, UUID userId, String username);
    List<ChatResponse> getChatHistory(UUID documentId);
    CompareResponse compareDocuments(CompareRequest request);
    long countTotalRequests(String username, String role);
}
