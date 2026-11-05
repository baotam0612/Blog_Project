package com.backlog.backend.service;

import com.backlog.backend.model.dto.AttachmentDownloadDTO;
import com.backlog.backend.model.dto.PostAttachmentDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostAttachmentService {
    PostAttachmentDTO upload(Long postId, MultipartFile file, String username);

    List<PostAttachmentDTO> getAllByPostId(Long postId);

    AttachmentDownloadDTO download(Long postId, Long attachmentId);

    void delete(Long postId, Long attachmentId, String username);

    void deleteAllByPostId(Long postId);
}
