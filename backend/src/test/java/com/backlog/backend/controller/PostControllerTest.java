package com.backlog.backend.controller;

import com.backlog.backend.config.SecurityConfig;
import com.backlog.backend.exception.ForbiddenException;
import com.backlog.backend.model.dto.PostDTO;
import com.backlog.backend.model.request.PostRequest;
import com.backlog.backend.model.request.UpdateStatusRequest;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.repository.InvalidatedTokenRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import com.backlog.backend.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PostController.class,
        properties = "jwt.signerKey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PostService postService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    UserRoleRepository userRoleRepository;

    @MockitoBean
    InvalidatedTokenRepository invalidatedTokenRepository;

    PostRequest postRequest;
    PostDTO postDTO;
    PostResponse postResponse;

    @BeforeEach
    void initData() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 8, 0);
        List<Long> tagIds = List.of(1L, 2L);
        List<String> tagNames = List.of("Java", "Spring Boot");

        postRequest = PostRequest.builder()
                .title("Test01")
                .slug("java-test")
                .excerpt("Bài viết dùng để test")
                .coverImageUrl("https://example.com/images/test01.jpg")
                .content("Nội dung bài viết test01")
                .status("DRAFT")
                .categoryId(1L)
                .tagIds(tagIds)
                .build();

        postDTO = PostDTO.builder()
                .id(1L)
                .title("Test01")
                .slug("java-test")
                .excerpt("Bài viết dùng để test")
                .coverImageUrl("https://example.com/images/test01.jpg")
                .content("Nội dung bài viết test01")
                .status("DRAFT")
                .createdAt(createdAt)
                .authorId(1L)
                .authorUsername("test-user")
                .authorDisplayName("Test User")
                .categoryId(1L)
                .categoryName("Programming")
                .tagIds(tagIds)
                .tagNames(tagNames)
                .build();

        postResponse = PostResponse.builder()
                .title("Test01")
                .slug("java-test")
                .excerpt("Bài viết dùng để test")
                .coverImageUrl("https://example.com/images/test01.jpg")
                .content("Nội dung bài viết test01")
                .status("DRAFT")
                .createdAt(createdAt)
                .username("Test User")
                .categoryName("Programming")
                .tagNames(tagNames)
                .build();
    }

    @Test
    void getAllPosts_Public_Success() throws Exception {
        when(postService.getAllPosts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(postResponse)));

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Find All Posts Successfully"))
                .andExpect(jsonPath("$.data.content[0].title").value("Test01"))
                .andExpect(jsonPath("$.data.content[0].tagNames[1]").value("Spring Boot"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).getAllPosts(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void createPost_Anonymous_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isUnauthorized());

        verify(postService, never()).createPost(any(PostRequest.class), any(String.class));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void createPost_ValidRequest_Success() throws Exception {
        when(postService.createPost(any(PostRequest.class), eq("test-user")))
                .thenReturn(postDTO);

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Create Post Successfully"))
                .andExpect(jsonPath("$.data.title").value("Test01"))
                .andExpect(jsonPath("$.data.username").value("Test User"))
                .andExpect(jsonPath("$.data.categoryName").value("Programming"))
                .andExpect(jsonPath("$.data.tagNames[0]").value("Java"));

        verify(postService).createPost(any(PostRequest.class), eq("test-user"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void createPost_InvalidRequest_BadRequest() throws Exception {
        postRequest.setTitle(" ");

        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("title")));

        verify(postService, never()).createPost(any(PostRequest.class), any(String.class));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void updatePostStatus_ValidRequest_Success() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("PUBLISHED");
        postDTO.setStatus("PUBLISHED");
        when(postService.updatePostStatus(eq(1L), any(UpdateStatusRequest.class), eq("test-user")))
                .thenReturn(postDTO);

        mockMvc.perform(patch("/api/v1/posts/1/change-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Update Post Status Successfully"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        verify(postService).updatePostStatus(eq(1L), any(UpdateStatusRequest.class), eq("test-user"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void updatePostStatus_InvalidStatus_BadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/posts/1/change-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(postService, never())
                .updatePostStatus(any(Long.class), any(UpdateStatusRequest.class), any(String.class));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void updatePost_ValidRequest_Success() throws Exception {
        when(postService.updatePost(eq(1L), any(PostRequest.class), eq("test-user")))
                .thenReturn(postDTO);

        mockMvc.perform(put("/api/v1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Update Post Successfully"))
                .andExpect(jsonPath("$.data.slug").value("java-test"));

        verify(postService).updatePost(eq(1L), any(PostRequest.class), eq("test-user"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void updatePost_NotOwner_Forbidden() throws Exception {
        when(postService.updatePost(eq(1L), any(PostRequest.class), eq("test-user")))
                .thenThrow(new ForbiddenException("User does not own this post"));

        mockMvc.perform(put("/api/v1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("User does not own this post"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void deletePost_Owner_Success() throws Exception {
        doNothing().when(postService).deletePostById(1L, "test-user");

        mockMvc.perform(delete("/api/v1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete Post Successfully"));

        verify(postService).deletePostById(1L, "test-user");
    }

    @Test
    @WithMockUser(username = "test-user", roles = "READER")
    void deletePost_NotOwner_Forbidden() throws Exception {
        doThrow(new ForbiddenException("User does not own this post"))
                .when(postService).deletePostById(1L, "test-user");

        mockMvc.perform(delete("/api/v1/posts/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void searchPost_Public_Success() throws Exception {
        when(postService.searchPostByTitleAndAuthor(any(Pageable.class), eq("java")))
                .thenReturn(new PageImpl<>(List.of(postDTO)));

        mockMvc.perform(get("/api/v1/posts/search")
                        .param("search", "java")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Search Post By Title And Author Successfully"))
                .andExpect(jsonPath("$.data.content[0].title").value("Test01"))
                .andExpect(jsonPath("$.data.content[0].username").value("Test User"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).searchPostByTitleAndAuthor(pageableCaptor.capture(), eq("java"));
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("title").isDescending());
    }
}
