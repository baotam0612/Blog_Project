package com.backlog.backend.service;

import com.backlog.backend.model.dto.PostDTO;
import com.backlog.backend.model.request.PostRequest;
import com.backlog.backend.model.request.UpdateStatusRequest;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Page<PostResponse> getAllPosts(Pageable pageable);

    PostDTO createPost(PostRequest postRequest, String username);

    PostDTO updatePostStatus(Long id, UpdateStatusRequest status, String username);

    PostDTO updatePost(Long id, PostRequest postRequest, String username);

    void deletePostById(Long id, String username);

    Page<PostDTO> searchPostByTitleAndAuthor(Pageable pageable, String search);
}
