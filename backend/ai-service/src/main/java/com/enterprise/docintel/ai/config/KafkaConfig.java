package com.enterprise.docintel.ai.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String DOCUMENT_PROCESSING_TOPIC = "document-processing";
    public static final String DOCUMENT_STATUS_TOPIC = "document-status";

    @Bean
    public NewTopic documentProcessingTopic() {
        return TopicBuilder.name(DOCUMENT_PROCESSING_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentStatusTopic() {
        return TopicBuilder.name(DOCUMENT_STATUS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
