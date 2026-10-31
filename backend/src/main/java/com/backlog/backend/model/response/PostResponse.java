package com.backlog.backend.model.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostResponse {
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
    List<String> tagname;
}
