package com.backlog.backend.kafka;

import com.backlog.backend.event.PostLikedEvent;
import com.backlog.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PostLikeEmailConsumer {

    private static final String PROCESSED_EVENT_PREFIX = "kafka:email:processed:";

    private final EmailService emailService;
    private final RedissonClient redissonClient;

    // Consumer nhận message
    @KafkaListener(
            topics = "${app.kafka.topic.post-liked:post-liked}",
            groupId = "${spring.kafka.consumer.group-id:blog-email-group}",
            autoStartup = "${app.kafka.listener-enabled:true}"
    )
    public void consume(PostLikedEvent event) {

        // Redis chống xử lý event trùng
        RBucket<String> processed = redissonClient.getBucket(
                PROCESSED_EVENT_PREFIX + event.getEventId()
        );

        // Nếu key tồn tại nghĩa là event đã được xử lý, consumer dừng và không gửi email lần nữa.
        if (processed.isExists()) {
            return;
        }

        // Gửi mail
        emailService.sendPostLikedEmail(event);
        processed.set("DONE", Duration.ofDays(7));
    }
}
