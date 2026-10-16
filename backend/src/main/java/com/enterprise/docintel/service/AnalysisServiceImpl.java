package com.enterprise.docintel.service;

import com.enterprise.docintel.ai.DocumentAiService;
import com.enterprise.docintel.dto.*;
import com.enterprise.docintel.entity.ChatHistory;
import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.DocumentAnalysis;
import com.enterprise.docintel.entity.User;
import com.enterprise.docintel.exception.ResourceNotFoundException;
import com.enterprise.docintel.repository.AnalysisRepository;
import com.enterprise.docintel.repository.ChatHistoryRepository;
import com.enterprise.docintel.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentAiService documentAiService;

    private void validateDocumentOwnership(Document doc, User user) {
        if (user != null && !user.getRole().equals(com.enterprise.docintel.entity.Role.ROLE_ADMIN) && !doc.getOwner().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Document not found or access denied");
        }
    }

    @Override
    public AnalysisResponse getAnalysisByDocumentId(UUID documentId, User user) {
        DocumentAnalysis analysis = analysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found for document ID: " + documentId));
        validateDocumentOwnership(analysis.getDocument(), user);
        return mapToResponse(analysis);
    }

    @Override
    public ChatResponse askQuestion(UUID documentId, ChatRequest request, User user) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
        validateDocumentOwnership(doc, user);

        String fileContent;
        try {
            fileContent = fileStorageService.extractText(doc.getFilePath(), doc.getFileType());
        } catch (IOException e) {
            log.error("Failed to read document for chat", e);
            fileContent = "[Error reading document content]";
        }

        // Real AI query
        String answer = documentAiService.askDocumentQuestion(fileContent, request.getQuestion());

        ChatHistory chatHistory = ChatHistory.builder()
                .document(doc)
                .user(user)
                .question(request.getQuestion())
                .answer(answer)
                .build();

        chatHistoryRepository.save(chatHistory);

        return new ChatResponse(chatHistory.getQuestion(), chatHistory.getAnswer(), chatHistory.getCreatedAt());
    }

    @Override
    public List<ChatResponse> getChatHistory(UUID documentId, User user) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
        validateDocumentOwnership(doc, user);

        return chatHistoryRepository.findByDocumentIdOrderByCreatedAtAsc(documentId).stream()
                .map(chat -> new ChatResponse(chat.getQuestion(), chat.getAnswer(), chat.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public CompareResponse compareDocuments(CompareRequest request, User user) {
        Document doc1 = documentRepository.findById(request.getDocumentId1())
                .orElseThrow(() -> new ResourceNotFoundException("Document 1 not found"));
        Document doc2 = documentRepository.findById(request.getDocumentId2())
                .orElseThrow(() -> new ResourceNotFoundException("Document 2 not found"));

        validateDocumentOwnership(doc1, user);
        validateDocumentOwnership(doc2, user);

        String content1, content2;
        try {
            content1 = fileStorageService.extractText(doc1.getFilePath(), doc1.getFileType());
            content2 = fileStorageService.extractText(doc2.getFilePath(), doc2.getFileType());
        } catch (IOException e) {
            log.error("Failed to read documents for comparison", e);
            content1 = "";
            content2 = "";
        }

        // Real comparison via Spring AI
        String comparisonReport = documentAiService.compareDocuments(content1, content2);

        return CompareResponse.builder()
                .doc1Name(doc1.getName())
                .doc2Name(doc2.getName())
                .comparisonReport("Completed comparison between Version 1 and Version 2.")
                .differences(comparisonReport)
                .build();
    }

    @Override
    public long countTotalRequests(User user) {
        if (user != null && !user.getRole().equals(com.enterprise.docintel.entity.Role.ROLE_ADMIN)) {
            return chatHistoryRepository.findByUserId(user.getId()).size() + documentRepository.findByOwner(user).size();
        }
        return analysisRepository.count() + chatHistoryRepository.count();
    }

    private AnalysisResponse mapToResponse(DocumentAnalysis analysis) {
        return AnalysisResponse.builder()
                .id(analysis.getId())
                .documentId(analysis.getDocument().getId())
                .summary(analysis.getSummary())
                .extractedData(analysis.getExtractedData())
                .possibleIssues(analysis.getPossibleIssues())
                .confidenceScore(analysis.getConfidenceScore())
                .processedAt(analysis.getProcessedAt())
                .build();
    }
}
