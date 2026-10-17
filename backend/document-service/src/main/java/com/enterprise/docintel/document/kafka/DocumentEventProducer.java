package com.enterprise.docintel.document.kafka;

import com.enterprise.docintel.common.dto.DocumentUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DocumentEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendDocumentUploadedEvent(DocumentUploadedEvent event) {
        log.info("Sending Kafka event for document upload: {}", event.getDocumentId());
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.DOCUMENT_PROCESSING_TOPIC, event.getDocumentId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to serialize DocumentUploadedEvent", e);
            throw new RuntimeException("Kafka event serialization error", e);
        }
    }
}
