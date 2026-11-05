package com.backlog.backend.model.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class PostCountDTO {

    Long postId;
    String title;
    long likeCount;
}
