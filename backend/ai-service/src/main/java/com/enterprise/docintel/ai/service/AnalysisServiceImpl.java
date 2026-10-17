package com.enterprise.docintel.ai.service;

import com.enterprise.docintel.ai.entity.ChatHistory;
import com.enterprise.docintel.ai.entity.DocumentAnalysis;
import com.enterprise.docintel.ai.repository.AnalysisRepository;
import com.enterprise.docintel.ai.repository.ChatHistoryRepository;
import com.enterprise.docintel.common.dto.*;
import com.enterprise.docintel.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisServiceImpl.class);

    private final AnalysisRepository analysisRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final DocumentAiService documentAiService;

    public AnalysisServiceImpl(AnalysisRepository analysisRepository,
                               ChatHistoryRepository chatHistoryRepository,
                               DocumentAiService documentAiService) {
        this.analysisRepository = analysisRepository;
        this.chatHistoryRepository = chatHistoryRepository;
        this.documentAiService = documentAiService;
    }

    @Override
    public AnalysisResponse getAnalysisByDocumentId(UUID documentId, String currentUser, String currentRole) {
        DocumentAnalysis analysis = analysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for document ID: " + documentId));
        return mapToResponse(analysis);
    }

    @Override
    public ChatResponse askQuestion(UUID documentId, ChatRequest request, UUID userId, String username) {
        DocumentAnalysis analysis = analysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for document ID: " + documentId));

        String fileContent = analysis.getExtractedText() != null ? analysis.getExtractedText() : "[No content available]";

        // AI Q&A via RAG
        String answer = documentAiService.askDocumentQuestion(fileContent, request.getQuestion());

        ChatHistory chatHistory = ChatHistory.builder()
                .documentId(documentId)
                .userId(userId)
                .username(username)
                .question(request.getQuestion())
                .answer(answer)
                .build();

        chatHistoryRepository.save(chatHistory);

        return new ChatResponse(chatHistory.getQuestion(), chatHistory.getAnswer(), chatHistory.getCreatedAt());
    }

    @Override
    public List<ChatResponse> getChatHistory(UUID documentId) {
        return chatHistoryRepository.findByDocumentIdOrderByCreatedAtAsc(documentId).stream()
                .map(chat -> new ChatResponse(chat.getQuestion(), chat.getAnswer(), chat.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public CompareResponse compareDocuments(CompareRequest request) {
        DocumentAnalysis doc1 = analysisRepository.findByDocumentId(request.getDocumentId1())
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for Document 1"));
        DocumentAnalysis doc2 = analysisRepository.findByDocumentId(request.getDocumentId2())
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for Document 2"));

        String content1 = doc1.getExtractedText() != null ? doc1.getExtractedText() : "";
        String content2 = doc2.getExtractedText() != null ? doc2.getExtractedText() : "";

        String comparisonReport = documentAiService.compareDocuments(content1, content2);

        return CompareResponse.builder()
                .doc1Name("Version 1 (Doc " + request.getDocumentId1().toString().substring(0, 8) + ")")
                .doc2Name("Version 2 (Doc " + request.getDocumentId2().toString().substring(0, 8) + ")")
                .comparisonReport("Completed comparison between Version 1 and Version 2.")
                .differences(comparisonReport)
                .documentId1(request.getDocumentId1())
                .documentId2(request.getDocumentId2())
                .build();
    }

    @Override
    public long countTotalRequests(String username, String role) {
        if (username != null && !"ROLE_ADMIN".equals(role)) {
            return chatHistoryRepository.findByUsername(username).size();
        }
        return analysisRepository.count() + chatHistoryRepository.count();
    }

    private AnalysisResponse mapToResponse(DocumentAnalysis analysis) {
        return AnalysisResponse.builder()
                .id(analysis.getId())
                .documentId(analysis.getDocumentId())
                .summary(analysis.getSummary())
                .extractedData(analysis.getExtractedData())
                .possibleIssues(analysis.getPossibleIssues())
                .confidenceScore(analysis.getConfidenceScore())
                .processedAt(analysis.getProcessedAt())
                .build();
    }
}
