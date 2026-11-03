package com.backlog.backend.model.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostDTO {

    Long id;
    String title;
    String slug;
    String excerpt;
    String coverImageUrl;
    String content;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime publishedAt;
    Long authorId;
    String authorUsername;
    String authorDisplayName;
    Long categoryId;
    String categoryName;
    List<Long> tagIds;
    List<String> tagNames;
}