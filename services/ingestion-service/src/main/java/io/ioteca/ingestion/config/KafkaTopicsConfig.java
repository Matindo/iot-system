package io.ioteca.ingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic rawIngestTopic() {
        return TopicBuilder.name("raw.ingest.af-ke-1")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic processedIngestTopic() {
        return TopicBuilder.name("processed.ingest.af-ke-1")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
