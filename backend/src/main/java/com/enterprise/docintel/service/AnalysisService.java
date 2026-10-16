package com.enterprise.docintel.service;

import com.enterprise.docintel.dto.*;
import com.enterprise.docintel.entity.User;
import java.util.List;
import java.util.UUID;

public interface AnalysisService {
    AnalysisResponse getAnalysisByDocumentId(UUID documentId, User user);
    ChatResponse askQuestion(UUID documentId, ChatRequest request, User user);
    List<ChatResponse> getChatHistory(UUID documentId, User user);
    CompareResponse compareDocuments(CompareRequest request, User user);
    long countTotalRequests(User user);
}
