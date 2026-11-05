package com.backlog.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAttachmentDTO {
    Long id;
    Long postId;
    String originalName;
    String contentType;
    Long fileSize;
    String downloadUrl;
    LocalDateTime createdAt;
}
