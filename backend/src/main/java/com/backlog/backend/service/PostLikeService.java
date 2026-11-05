package com.backlog.backend.service;


import com.backlog.backend.model.dto.PostCountDTO;
import com.backlog.backend.model.dto.PostLikeDTO;

public interface PostLikeService {
    PostLikeDTO doLike(Long postId, String username);

    void deleteLikeByIdAndUsername(Long postId, String username);

    PostCountDTO getLikeCount(Long postId);
}
