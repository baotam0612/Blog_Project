package com.backlog.backend.controller;

import com.backlog.backend.config.SecurityConfig;
import com.backlog.backend.exception.ForbiddenException;
import com.backlog.backend.model.dto.AttachmentDownloadDTO;
import com.backlog.backend.model.dto.PostAttachmentDTO;
import com.backlog.backend.service.TokenBlacklistService;
import com.backlog.backend.service.UserAuthCacheService;
import com.backlog.backend.service.PostAttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PostAttachmentController.class,
        properties = "jwt.signerKey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
)
@Import(SecurityConfig.class)
class PostAttachmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostAttachmentService attachmentService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;
    @MockitoBean
    UserAuthCacheService userAuthCacheService;

    PostAttachmentDTO attachment;

    @BeforeEach
    void initData() {
        attachment = PostAttachmentDTO.builder()
                .id(5L)
                .postId(1L)
                .originalName("document.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .fileSize(4L)
                .downloadUrl("/api/v1/posts/1/attachments/5/download")
                .createdAt(LocalDateTime.of(2026, 1, 1, 8, 0))
                .build();
    }

    @Test
    void upload_Anonymous_Unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/posts/1/attachments").file(file))
                .andExpect(status().isUnauthorized());

        verify(attachmentService, never()).upload(any(Long.class), any(), any(String.class));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void upload_Owner_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test".getBytes()
        );
        when(attachmentService.upload(eq(1L), any(), eq("test-user")))
                .thenReturn(attachment);

        mockMvc.perform(multipart("/api/v1/posts/1/attachments").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Upload Attachment Successfully"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.originalName").value("document.pdf"))
                .andExpect(jsonPath("$.data.downloadUrl")
                        .value("/api/v1/posts/1/attachments/5/download"));

        verify(attachmentService).upload(eq(1L), any(), eq("test-user"));
    }

    @Test
    void getAll_Public_Success() throws Exception {
        when(attachmentService.getAllByPostId(1L)).thenReturn(List.of(attachment));

        mockMvc.perform(get("/api/v1/posts/1/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Get Post Attachments Successfully"))
                .andExpect(jsonPath("$.data[0].originalName").value("document.pdf"));
    }

    @Test
    void download_Public_Success() throws Exception {
        byte[] fileContent = "test".getBytes();
        when(attachmentService.download(1L, 5L)).thenReturn(
                new AttachmentDownloadDTO(
                        new ByteArrayResource(fileContent),
                        "document.pdf",
                        MediaType.APPLICATION_PDF_VALUE
                )
        );

        mockMvc.perform(get("/api/v1/posts/1/attachments/5/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().bytes(fileContent));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void delete_Owner_Success() throws Exception {
        doNothing().when(attachmentService).delete(1L, 5L, "test-user");

        mockMvc.perform(delete("/api/v1/posts/1/attachments/5/remove"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete Attachment Successfully"));

        verify(attachmentService).delete(1L, 5L, "test-user");
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void delete_NotOwner_Forbidden() throws Exception {
        doThrow(new ForbiddenException("User does not own this post"))
                .when(attachmentService).delete(1L, 5L, "test-user");

        mockMvc.perform(delete("/api/v1/posts/1/attachments/5/remove"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
