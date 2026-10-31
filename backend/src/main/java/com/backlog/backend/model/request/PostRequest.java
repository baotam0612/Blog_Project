package com.backlog.backend.model.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostRequest {

    String title;
    String slug;
    String excerpt;
    String coverImageUrl;
    String content;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime pushlishedAt;
    String username;
    String categoryName;
}
