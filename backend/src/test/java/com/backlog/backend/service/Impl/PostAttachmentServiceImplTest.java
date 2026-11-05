package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.PostAttachmentEntity;
import com.backlog.backend.entity.PostEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.exception.ForbiddenException;
import com.backlog.backend.model.dto.AttachmentDownloadDTO;
import com.backlog.backend.model.dto.PostAttachmentDTO;
import com.backlog.backend.repository.PostAttachmentRepository;
import com.backlog.backend.repository.PostRepository;
import com.backlog.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostAttachmentServiceImplTest {

    @Mock
    PostAttachmentRepository attachmentRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    UserRepository userRepository;

    @TempDir
    Path uploadDirectory;

    PostAttachmentServiceImpl attachmentService;
    UserEntity owner;
    PostEntity post;

    @BeforeEach
    void setUp() {
        attachmentService = new PostAttachmentServiceImpl(
                attachmentRepository,
                postRepository,
                userRepository,
                uploadDirectory.toString(),
                10 * 1024 * 1024
        );
        owner = UserEntity.builder().id(1L).username("owner").build();
        post = PostEntity.builder().id(10L).userPost(owner).build();
    }

    @Test
    void upload_ValidFile_SavesFileAndMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "file-content".getBytes()
        );
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(attachmentRepository.save(any(PostAttachmentEntity.class))).thenAnswer(invocation -> {
            PostAttachmentEntity attachment = invocation.getArgument(0);
            attachment.setId(5L);
            attachment.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
            return attachment;
        });

        PostAttachmentDTO result = attachmentService.upload(10L, file, "owner");

        assertEquals(5L, result.getId());
        assertEquals("document.pdf", result.getOriginalName());
        assertEquals("/api/v1/posts/10/attachments/5/download", result.getDownloadUrl());

        ArgumentCaptor<PostAttachmentEntity> captor =
                ArgumentCaptor.forClass(PostAttachmentEntity.class);
        verify(attachmentRepository).save(captor.capture());
        Path storedFile = uploadDirectory.resolve(captor.getValue().getFilePath());
        assertTrue(Files.exists(storedFile));
        assertArrayEquals("file-content".getBytes(), Files.readAllBytes(storedFile));
    }

    @Test
    void upload_NotOwner_ThrowsForbiddenAndDoesNotSave() {
        UserEntity anotherUser = UserEntity.builder().id(2L).username("another-user").build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "file-content".getBytes()
        );
        when(userRepository.findByUsername("another-user"))
                .thenReturn(Optional.of(anotherUser));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        assertThrows(
                ForbiddenException.class,
                () -> attachmentService.upload(10L, file, "another-user")
        );

        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void upload_UnsupportedContentType_Throws415() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "file-content".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> attachmentService.upload(10L, file, "owner")
        );

        assertEquals(415, exception.getStatusCode().value());
        verify(postRepository, never()).findById(any());
    }

    @Test
    void download_ExistingFile_ReturnsResource() throws Exception {
        Path postDirectory = Files.createDirectories(uploadDirectory.resolve("10"));
        Path storedFile = postDirectory.resolve("stored.pdf");
        Files.write(storedFile, "file-content".getBytes());
        PostAttachmentEntity attachment = attachment("10/stored.pdf");
        when(attachmentRepository.findByIdAndPost_Id(5L, 10L))
                .thenReturn(Optional.of(attachment));

        AttachmentDownloadDTO result = attachmentService.download(10L, 5L);

        assertEquals("document.pdf", result.getOriginalName());
        assertTrue(result.getResource().exists());
        try (InputStream inputStream = result.getResource().getInputStream()) {
            assertArrayEquals("file-content".getBytes(), inputStream.readAllBytes());
        }
    }

    @Test
    void delete_Owner_RemovesFileAndMetadata() throws Exception {
        Path postDirectory = Files.createDirectories(uploadDirectory.resolve("10"));
        Path storedFile = postDirectory.resolve("stored.pdf");
        Files.write(storedFile, "file-content".getBytes());
        PostAttachmentEntity attachment = attachment("10/stored.pdf");
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(attachmentRepository.findByIdAndPost_Id(5L, 10L))
                .thenReturn(Optional.of(attachment));

        attachmentService.delete(10L, 5L, "owner");

        assertFalse(Files.exists(storedFile));
        verify(attachmentRepository).delete(attachment);
    }

    private PostAttachmentEntity attachment(String filePath) {
        return PostAttachmentEntity.builder()
                .id(5L)
                .originalName("document.pdf")
                .storedName("stored.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .fileSize(12L)
                .filePath(filePath)
                .createdAt(LocalDateTime.of(2026, 1, 1, 8, 0))
                .post(post)
                .build();
    }
}
