package com.backlog.backend.controller;


import com.backlog.backend.Response.PayloadResponse;
import com.backlog.backend.model.dto.PostDTO;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {
    PostService postService;

    // Get Post
    @GetMapping("/posts")
    public PayloadResponse getAllPosts(){
        List<PostResponse> res = postService.getAllPosts();
        return new PayloadResponse(HttpStatus.OK, "Find All Posts Successfully", res);
    }

}
