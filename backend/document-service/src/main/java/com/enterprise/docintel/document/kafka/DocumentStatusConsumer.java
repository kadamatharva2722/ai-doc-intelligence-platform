package com.enterprise.docintel.document.kafka;

import com.enterprise.docintel.common.dto.DocumentStatusUpdatedEvent;
import com.enterprise.docintel.document.entity.Document;
import com.enterprise.docintel.document.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DocumentStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentStatusConsumer.class);

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public DocumentStatusConsumer(DocumentRepository documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.DOCUMENT_STATUS_TOPIC, groupId = "${spring.kafka.consumer.group-id:docintel-document-group}")
    public void consumeStatusUpdate(String message) {
        log.info("Received document status update event: {}", message);
        try {
            DocumentStatusUpdatedEvent event = objectMapper.readValue(message, DocumentStatusUpdatedEvent.class);
            Document doc = documentRepository.findById(event.getDocumentId()).orElse(null);
            if (doc != null) {
                doc.setStatus(event.getStatus());
                documentRepository.save(doc);
                log.info("Updated document {} status to {}", doc.getId(), event.getStatus());
            } else {
                log.warn("Document not found for status update: {}", event.getDocumentId());
            }
        } catch (Exception e) {
            log.error("Failed to process document status update event", e);
        }
    }
}
