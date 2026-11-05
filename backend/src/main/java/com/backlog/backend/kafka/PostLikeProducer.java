package com.backlog.backend.kafka;

import com.backlog.backend.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostLikeProducer {

    private final KafkaTemplate<String, PostLikedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.post-liked:post-liked}")
    private String topic;

    public void send(PostLikedEvent event) {

        //KafkaTemplate là công cụ Spring cung cấp để gửi message.
        kafkaTemplate.send(topic, event.getPostId().toString(), event)
                //Topic: post-liked
                //Key: "10"
                //Value:
                //{
                //    "eventId": "e77bf...",
                //    "postId": 10,
                //    "postTitle": "Học Spring Boot",
                //    "ownerEmail": "owner@gmail.com",
                //    "likedByUsername": "user01"
                //}
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Cannot publish PostLikedEvent {}", event.getEventId(), exception);
                    } else {
                        log.info("Published PostLikedEvent {} to partition {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
