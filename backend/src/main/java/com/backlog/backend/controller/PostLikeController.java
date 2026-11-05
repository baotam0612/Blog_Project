package com.backlog.backend.controller;


import com.backlog.backend.Response.PayloadResponse;
import com.backlog.backend.model.dto.PostCountDTO;
import com.backlog.backend.model.dto.PostLikeDTO;
import com.backlog.backend.model.response.PostCountResponse;
import com.backlog.backend.model.response.PostLikeResponse;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.service.PostLikeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostLikeController {

    PostLikeService postLikeService;

    PostLikeResponse toResponse(PostLikeDTO postLikeDTO){
        return PostLikeResponse.builder()
                .title(postLikeDTO.getTitle())
                .slug(postLikeDTO.getSlug())
                .username(postLikeDTO.getUsername())
                .displayname(postLikeDTO.getDisplayname())
                .build();
    }

    // Like Post
    @PostMapping("/posts/{postId}/likes")
    public PayloadResponse likePost(@PathVariable Long postId, Authentication authentication
    ){
        PostLikeResponse postLikeResponse = toResponse(postLikeService.doLike(postId, authentication.getName()));
        return new PayloadResponse(HttpStatus.CREATED, "Like Successfully", postLikeResponse);
    }

    // Delete Like Post
    @DeleteMapping("/posts/{postId}/likes/remove")
    public PayloadResponse deleteLike(@PathVariable Long postId, Authentication authentication){
        postLikeService.deleteLikeByIdAndUsername(postId, authentication.getName());
        return new PayloadResponse(HttpStatus.OK,"Delete Successfully","");
    }

    // Count Like Post
    @GetMapping("/posts/{postId}/likes/count")
    public PayloadResponse getLikeCount(@PathVariable Long postId){
        PostCountDTO postCountDTO = postLikeService.getLikeCount(postId);
        PostCountResponse res = PostCountResponse.builder()
                .title(postCountDTO.getTitle())
                .likeCount(postCountDTO.getLikeCount())
                .build();
        return new PayloadResponse(HttpStatus.OK,"get count successfully", res);
    }
}
