package com.enterprise.docintel.service;

import com.enterprise.docintel.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendDocumentUploadedEvent(UUID documentId) {
        log.info("Sending Kafka event for document upload: {}", documentId);
        kafkaTemplate.send(KafkaConfig.DOCUMENT_PROCESSING_TOPIC, documentId.toString());
    }
}
