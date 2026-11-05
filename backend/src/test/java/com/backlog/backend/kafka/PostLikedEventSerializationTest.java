package com.backlog.backend.kafka;

import com.backlog.backend.event.PostLikedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PostLikedEventSerializationTest {

    @Test
    void serializeEventWithLocalDateTime() {
        PostLikedEvent event = PostLikedEvent.builder()
                .eventId("event-1")
                .postId(4L)
                .postTitle("Xác thực JWT trong Spring Boot")
                .ownerEmail("owner@example.com")
                .likedByUsername("reader")
                .likedAt(LocalDateTime.of(2026, 11, 5, 10, 42, 41))
                .build();

        JacksonJsonSerializer<PostLikedEvent> serializer = new JacksonJsonSerializer<>();
        byte[] payload = serializer.serialize("post-liked", event);
        serializer.close();

        String json = new String(payload, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"likedAt\":\"2026-11-05T10:42:41\""));
    }
}
