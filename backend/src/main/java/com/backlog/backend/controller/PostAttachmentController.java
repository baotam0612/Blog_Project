package com.backlog.backend.controller;

import com.backlog.backend.Response.PayloadResponse;
import com.backlog.backend.model.dto.AttachmentDownloadDTO;
import com.backlog.backend.model.dto.PostAttachmentDTO;
import com.backlog.backend.model.response.PostAttachmentResponse;
import com.backlog.backend.service.PostAttachmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/attachments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostAttachmentController {

    PostAttachmentService attachmentService;

    // Upload File
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PayloadResponse upload(
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        PostAttachmentDTO attachment = attachmentService.upload(
                postId,
                file,
                authentication.getName()
        );
        return new PayloadResponse(
                HttpStatus.CREATED,
                "Upload Attachment Successfully",
                toResponse(attachment)
        );
    }

    // Get All File
    @GetMapping
    public PayloadResponse getAll(@PathVariable Long postId) {
        List<PostAttachmentResponse> attachments = attachmentService
                .getAllByPostId(postId)
                .stream()
                .map(this::toResponse)
                .toList();

        return new PayloadResponse(
                HttpStatus.OK,
                "Get Post Attachments Successfully",
                attachments
        );
    }

    // Download File
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long postId,
            @PathVariable Long attachmentId
    ) {
        AttachmentDownloadDTO attachment = attachmentService.download(postId, attachmentId);
        MediaType mediaType = parseMediaType(attachment.getContentType());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(attachment.getResource());
    }

    // Delete File
    @DeleteMapping("/{attachmentId}/remove")
    public PayloadResponse delete(
            @PathVariable Long postId,
            @PathVariable Long attachmentId,
            Authentication authentication
    ) {
        attachmentService.delete(postId, attachmentId, authentication.getName());
        return new PayloadResponse(HttpStatus.OK, "Delete Attachment Successfully");
    }

    private PostAttachmentResponse toResponse(PostAttachmentDTO attachment) {
        return PostAttachmentResponse.builder()
                .id(attachment.getId())
                .originalName(attachment.getOriginalName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .downloadUrl(attachment.getDownloadUrl())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
