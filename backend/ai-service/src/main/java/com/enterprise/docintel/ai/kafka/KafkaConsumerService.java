package com.enterprise.docintel.ai.kafka;

import com.enterprise.docintel.ai.config.KafkaConfig;
import com.enterprise.docintel.ai.entity.DocumentAnalysis;
import com.enterprise.docintel.ai.repository.AnalysisRepository;
import com.enterprise.docintel.ai.service.DocumentAiService;
import com.enterprise.docintel.ai.service.TextExtractionService;
import com.enterprise.docintel.common.dto.DocumentStatusUpdatedEvent;
import com.enterprise.docintel.common.dto.DocumentUploadedEvent;
import com.enterprise.docintel.common.entity.DocumentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final AnalysisRepository analysisRepository;
    private final TextExtractionService textExtractionService;
    private final DocumentAiService documentAiService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(AnalysisRepository analysisRepository,
                                TextExtractionService textExtractionService,
                                DocumentAiService documentAiService,
                                KafkaProducerService kafkaProducerService,
                                ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.textExtractionService = textExtractionService;
        this.documentAiService = documentAiService;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.DOCUMENT_PROCESSING_TOPIC, groupId = "${spring.kafka.consumer.group-id:docintel-ai-group}")
    public void consumeDocumentUploadedEvent(String message) {
        log.info("Received Kafka event for AI document processing: {}", message);
        DocumentUploadedEvent event;
        try {
            event = objectMapper.readValue(message, DocumentUploadedEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse DocumentUploadedEvent: {}", message, e);
            return;
        }

        try {
            // 1. Notify that processing started
            kafkaProducerService.sendDocumentStatusUpdatedEvent(DocumentStatusUpdatedEvent.builder()
                    .documentId(event.getDocumentId())
                    .status(DocumentStatus.PROCESSING)
                    .build());

            // 2. Extract file contents from shared volume
            String textContent = textExtractionService.extractText(event.getFilePath(), event.getFileType());

            // 3. Run AI analysis
            String summary = documentAiService.generateSummary(textContent);
            String extractedData = documentAiService.extractStructuredData(textContent);
            String possibleIssues = documentAiService.identifyIssuesAndConfidence(textContent);

            Double confidenceScore = 0.95;
            try {
                if (possibleIssues != null && possibleIssues.contains("Confidence Score:")) {
                    int index = possibleIssues.indexOf("Confidence Score:");
                    String scoreStr = possibleIssues.substring(index + 17).trim();
                    if (scoreStr.length() >= 4) {
                        confidenceScore = Double.parseDouble(scoreStr.substring(0, 4));
                    }
                }
            } catch (Exception ignore) {}

            DocumentAnalysis analysis = DocumentAnalysis.builder()
                    .documentId(event.getDocumentId())
                    .summary(summary)
                    .extractedData(extractedData)
                    .possibleIssues(possibleIssues)
                    .extractedText(textContent)
                    .confidenceScore(confidenceScore)
                    .processedAt(LocalDateTime.now())
                    .build();

            analysisRepository.save(analysis);

            // 4. Notify that processing completed
            kafkaProducerService.sendDocumentStatusUpdatedEvent(DocumentStatusUpdatedEvent.builder()
                    .documentId(event.getDocumentId())
                    .status(DocumentStatus.COMPLETED)
                    .build());

            log.info("Successfully analyzed document ID: {}", event.getDocumentId());

        } catch (IOException e) {
            log.error("Failed to parse and extract text for document: {}", event.getName(), e);
            kafkaProducerService.sendDocumentStatusUpdatedEvent(DocumentStatusUpdatedEvent.builder()
                    .documentId(event.getDocumentId())
                    .status(DocumentStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build());
        } catch (Exception ex) {
            log.error("Unexpected error in async processing of document: {}", event.getName(), ex);
            kafkaProducerService.sendDocumentStatusUpdatedEvent(DocumentStatusUpdatedEvent.builder()
                    .documentId(event.getDocumentId())
                    .status(DocumentStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .build());
        }
    }
}
