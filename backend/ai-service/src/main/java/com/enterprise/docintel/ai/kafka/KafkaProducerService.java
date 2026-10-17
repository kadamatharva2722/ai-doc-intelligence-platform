package com.enterprise.docintel.ai.kafka;

import com.enterprise.docintel.ai.config.KafkaConfig;
import com.enterprise.docintel.common.dto.DocumentStatusUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendDocumentStatusUpdatedEvent(DocumentStatusUpdatedEvent event) {
        log.info("Sending Kafka event for document status: {} -> {}", event.getDocumentId(), event.getStatus());
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaConfig.DOCUMENT_STATUS_TOPIC, event.getDocumentId().toString(), json);
        } catch (Exception e) {
            log.error("Failed to serialize DocumentStatusUpdatedEvent", e);
        }
    }
}
