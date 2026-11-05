package com.backlog.backend.model.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostLikeDTO {
    Long id;

    Long postId;
    String title;
    String slug;

    Long userId;
    String username;
    String displayname;

    LocalDateTime likedAt;

}
