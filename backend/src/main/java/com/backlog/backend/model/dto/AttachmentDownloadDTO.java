package com.backlog.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
public class AttachmentDownloadDTO {
    Resource resource;
    String originalName;
    String contentType;
}
