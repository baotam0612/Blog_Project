package com.backlog.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic postLikedTopic(
            @Value("${app.kafka.topic.post-liked:post-liked}") String topicName,
            @Value("${app.kafka.topic.post-liked-partitions:1}") int partitions
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
