package com.backlog.backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLikedEvent {

    String eventId;
    Long postId;
    String postTitle;
    String postSlug;
    Long ownerId;
    String ownerEmail;
    Long likedByUserId;
    String likedByUsername;
    String likedByDisplayName;
    LocalDateTime likedAt;
}
