package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.PostAttachmentEntity;
import com.backlog.backend.entity.PostEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.exception.ForbiddenException;
import com.backlog.backend.exception.ResourceNotFoundException;
import com.backlog.backend.model.dto.AttachmentDownloadDTO;
import com.backlog.backend.model.dto.PostAttachmentDTO;
import com.backlog.backend.repository.PostAttachmentRepository;
import com.backlog.backend.repository.PostRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.service.PostAttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PostAttachmentServiceImpl implements PostAttachmentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final PostAttachmentRepository attachmentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final Path uploadRoot;
    private final long maxFileSize;

    public PostAttachmentServiceImpl(
            PostAttachmentRepository attachmentRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            @Value("${app.upload.dir:uploads}") String uploadDirectory,
            @Value("${app.upload.max-file-size:10485760}") long maxFileSize
    ) {
        this.attachmentRepository = attachmentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.uploadRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    @Override
    @Transactional
    public PostAttachmentDTO upload(Long postId, MultipartFile file, String username) {
        validateFile(file);
        PostEntity post = getOwnedPost(postId, username);

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + getExtension(originalName);
        String relativePath = postId + "/" + storedName;
        Path destination = resolveStoragePath(relativePath);

        try {
            Files.createDirectories(destination.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            PostAttachmentEntity attachment = PostAttachmentEntity.builder()
                    .originalName(originalName)
                    .storedName(storedName)
                    .contentType(normalizeContentType(file.getContentType()))
                    .fileSize(file.getSize())
                    .filePath(relativePath)
                    .post(post)
                    .build();

            return toDTO(attachmentRepository.save(attachment));
        } catch (IOException ex) {
            deleteFileQuietly(destination);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể lưu tệp đính kèm",
                    ex
            );
        } catch (RuntimeException ex) {
            deleteFileQuietly(destination);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostAttachmentDTO> getAllByPostId(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found");
        }

        return attachmentRepository.findAllByPost_IdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadDTO download(Long postId, Long attachmentId) {
        PostAttachmentEntity attachment = getAttachment(postId, attachmentId);
        Path path = resolveStoragePath(attachment.getFilePath());

        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Attachment file not found");
            }
            return new AttachmentDownloadDTO(
                    resource,
                    attachment.getOriginalName(),
                    attachment.getContentType()
            );
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Attachment file not found");
        }
    }

    @Override
    @Transactional
    public void delete(Long postId, Long attachmentId, String username) {
        getOwnedPost(postId, username);
        PostAttachmentEntity attachment = getAttachment(postId, attachmentId);

        deleteStoredFile(attachment.getFilePath());
        attachmentRepository.delete(attachment);
    }

    @Override
    @Transactional
    public void deleteAllByPostId(Long postId) {
        List<PostAttachmentEntity> attachments =
                attachmentRepository.findAllByPost_IdOrderByCreatedAtDesc(postId);

        attachments.forEach(attachment -> deleteStoredFile(attachment.getFilePath()));
        attachmentRepository.deleteAll(attachments);
    }

    private PostEntity getOwnedPost(Long postId, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!Objects.equals(post.getUserPost().getId(), user.getId())) {
            throw new ForbiddenException("User does not own this post");
        }
        return post;
    }

    private PostAttachmentEntity getAttachment(Long postId, Long attachmentId) {
        return attachmentRepository.findByIdAndPost_Id(attachmentId, postId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp đính kèm không được để trống");
        }
        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp đính kèm vượt quá dung lượng cho phép");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Loại tệp đính kèm không được hỗ trợ");
        }
    }

    private String sanitizeFileName(String fileName) {
        String safeName = Paths.get(fileName == null ? "file" : fileName)
                .getFileName()
                .toString()
                .trim();
        if (safeName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tệp không hợp lệ");
        }
        return safeName;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType.toLowerCase(Locale.ROOT);
    }

    private Path resolveStoragePath(String relativePath) {
        Path path = uploadRoot.resolve(relativePath).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đường dẫn tệp không hợp lệ");
        }
        return path;
    }

    private void deleteStoredFile(String relativePath) {
        Path path = resolveStoragePath(relativePath);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể xóa tệp đính kèm",
                    ex
            );
        }
    }

    private void deleteFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Keep the original exception that caused the upload to fail.
        }
    }

    private PostAttachmentDTO toDTO(PostAttachmentEntity attachment) {
        Long postId = attachment.getPost().getId();
        return PostAttachmentDTO.builder()
                .id(attachment.getId())
                .postId(postId)
                .originalName(attachment.getOriginalName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .downloadUrl("/api/v1/posts/" + postId
                        + "/attachments/" + attachment.getId() + "/download")
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
