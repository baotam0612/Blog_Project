package com.backlog.backend.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAttachmentResponse {
    Long id;
    String originalName;
    String contentType;
    Long fileSize;
    String downloadUrl;
    LocalDateTime createdAt;
}
