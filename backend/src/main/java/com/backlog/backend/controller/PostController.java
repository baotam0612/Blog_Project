package com.backlog.backend.controller;

import com.backlog.backend.Response.PayloadResponse;
import com.backlog.backend.model.dto.PostDTO;
import com.backlog.backend.model.request.PostRequest;
import com.backlog.backend.model.request.UpdateStatusRequest;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {
    PostService postService;

    private PostResponse toResponse(PostDTO postDTO) {
        return PostResponse.builder()
                .title(postDTO.getTitle())
                .slug(postDTO.getSlug())
                .excerpt(postDTO.getExcerpt())
                .coverImageUrl(postDTO.getCoverImageUrl())
                .content(postDTO.getContent())
                .status(postDTO.getStatus())
                .createdAt(postDTO.getCreatedAt())
                .updatedAt(postDTO.getUpdatedAt())
                .publishedAt(postDTO.getPublishedAt())
                .username(postDTO.getAuthorDisplayName())
                .categoryName(postDTO.getCategoryName())
                .tagNames(postDTO.getTagNames())
                .build();
    }

    // Get All Posts
    @GetMapping("/posts")
    public PayloadResponse getAllPosts(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page,size);
        Page<PostResponse> res = postService.getAllPosts(pageable);
        return new PayloadResponse(HttpStatus.OK, "Find All Posts Successfully", res);
    }

    // Create Post
    @PostMapping("/posts")
    public PayloadResponse createPost(
            @Valid @RequestBody PostRequest postRequest,
            Authentication authentication
    ) {
        PostDTO postDTO = postService.createPost(postRequest, authentication.getName());
        return new PayloadResponse(
                HttpStatus.CREATED,
                "Create Post Successfully",
                toResponse(postDTO)
        );
    }

    // Update Post Status
    @PatchMapping("/posts/{id}/change-status")
    public PayloadResponse updatePostStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication
    ) {
        PostDTO postDTO = postService.updatePostStatus(
                id,
                request,
                authentication.getName()
        );
        return new PayloadResponse(
                HttpStatus.OK,
                "Update Post Status Successfully",
                toResponse(postDTO)
        );
    }

    // Update Post
    @PutMapping("/posts/{id}")
    public PayloadResponse updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest postRequest,
            Authentication authentication
    ) {
        PostDTO postDTO = postService.updatePost(
                id,
                postRequest,
                authentication.getName()
        );
        return new PayloadResponse(
                HttpStatus.OK,
                "Update Post Successfully",
                toResponse(postDTO)
        );
    }

    // Delete Post
    @DeleteMapping("/posts/{id}")
    public PayloadResponse deletePost(
            @PathVariable Long id,
            Authentication authentication
    ) {
        postService.deletePostById(id, authentication.getName());
        return new PayloadResponse(HttpStatus.OK, "Delete Post Successfully");
    }

    // Search Post
    @GetMapping("/posts/search")
    public PayloadResponse searchPostByTitleAndAuthor(
            Pageable pageable,
            @RequestParam String search
    ) {
        Page<PostDTO> postDTOList = postService.searchPostByTitleAndAuthor(pageable, search);
        Page<PostResponse> res = postDTOList.map(this::toResponse);
        return new PayloadResponse(
                HttpStatus.OK,
                "Search Post By Title And Author Successfully",
                res
        );
    }

}
