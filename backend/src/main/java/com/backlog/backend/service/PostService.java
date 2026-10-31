package com.backlog.backend.service;

import com.backlog.backend.model.dto.PostDTO;
import com.backlog.backend.model.response.PostResponse;

import java.util.List;

public interface PostService {
    List<PostResponse> getAllPosts();
}
