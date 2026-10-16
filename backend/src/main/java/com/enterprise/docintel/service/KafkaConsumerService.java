package com.enterprise.docintel.service;

import com.enterprise.docintel.ai.DocumentAiService;
import com.enterprise.docintel.config.KafkaConfig;
import com.enterprise.docintel.entity.Document;
import com.enterprise.docintel.entity.DocumentAnalysis;
import com.enterprise.docintel.entity.DocumentStatus;
import com.enterprise.docintel.repository.AnalysisRepository;
import com.enterprise.docintel.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final DocumentRepository documentRepository;
    private final AnalysisRepository analysisRepository;
    private final FileStorageService fileStorageService;
    private final DocumentAiService documentAiService;

    @KafkaListener(topics = KafkaConfig.DOCUMENT_PROCESSING_TOPIC, groupId = "${spring.kafka.consumer.group-id:docintel-group}")
    public void consumeDocumentUploadedEvent(String documentIdStr) {
        log.info("Received Kafka event for document processing: {}", documentIdStr);
        UUID documentId = UUID.fromString(documentIdStr);

        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.error("Document not found with ID: {}", documentId);
            return;
        }

        try {
            // Set status to PROCESSING
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            // Extract file contents
            String textContent = fileStorageService.extractText(document.getFilePath(), document.getFileType());

            // Run analysis via Spring AI
            String summary = documentAiService.generateSummary(textContent);
            String extractedData = documentAiService.extractStructuredData(textContent);
            String possibleIssues = documentAiService.identifyIssuesAndConfidence(textContent);

            Double confidenceScore = 0.95;
            try {
                if (possibleIssues.contains("Confidence Score:")) {
                    int index = possibleIssues.indexOf("Confidence Score:");
                    String scoreStr = possibleIssues.substring(index + 17).trim();
                    if (scoreStr.length() >= 4) {
                        confidenceScore = Double.parseDouble(scoreStr.substring(0, 4));
                    }
                }
            } catch (Exception ignore) {}

            DocumentAnalysis analysis = DocumentAnalysis.builder()
                    .document(document)
                    .summary(summary)
                    .extractedData(extractedData)
                    .possibleIssues(possibleIssues)
                    .extractedText(textContent)
                    .confidenceScore(confidenceScore)
                    .processedAt(LocalDateTime.now())
                    .build();

            analysisRepository.save(analysis);

            // Set status to COMPLETED
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);

            log.info("Successfully analyzed document: {}", document.getName());
        } catch (IOException e) {
            log.error("Failed to parse and extract text for document: {}", document.getName(), e);
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
        } catch (Exception ex) {
            log.error("Unexpected error in async processing of document: {}", document.getName(), ex);
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);
        }
    }
}
