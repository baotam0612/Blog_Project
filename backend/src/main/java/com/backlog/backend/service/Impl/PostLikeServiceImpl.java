package com.backlog.backend.service.Impl;


import com.backlog.backend.entity.PostEntity;
import com.backlog.backend.entity.PostLikeEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.event.PostLikedEvent;
import com.backlog.backend.exception.ResourceNotFoundException;
import com.backlog.backend.model.dto.PostCountDTO;
import com.backlog.backend.model.dto.PostLikeDTO;
import com.backlog.backend.repository.PostLikeRepository;
import com.backlog.backend.repository.PostRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.service.PostLikeService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostLikeServiceImpl implements PostLikeService {

    PostRepository postRepository;
    UserRepository userRepository;
    PostLikeRepository postLikeRepository;
    ApplicationEventPublisher eventPublisher;



    @Transactional
    @Override
    public PostLikeDTO doLike(Long postId, String username) {
        PostEntity postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found to doLike"));

        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found to doLike"));

        if (postLikeRepository.existsByUserEntity_UsernameAndPostEntity_Id(username, postId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã like bài viết này");
        }

        PostLikeEntity postLikeEntity = new PostLikeEntity();
        postLikeEntity.setUserEntity(userEntity);
        postLikeEntity.setPostEntity(postEntity);

        postLikeRepository.saveAndFlush(postLikeEntity);

        UserEntity owner = postEntity.getUserPost();

        // Nếu người khác like, tạo PostLikedEvent
        if (!Objects.equals(owner.getId(), userEntity.getId())) {
            eventPublisher.publishEvent(PostLikedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .postId(postEntity.getId())
                    .postTitle(postEntity.getTitle())
                    .postSlug(postEntity.getSlug())
                    .ownerId(owner.getId())
                    .ownerEmail(owner.getEmail())
                    .likedByUserId(userEntity.getId())
                    .likedByUsername(userEntity.getUsername())
                    .likedByDisplayName(userEntity.getDisplayName())
                    .likedAt(postLikeEntity.getCreatedDate())
                    .build());
        }

        // Nếu chủ bài viết tự like thì không tạo event và không gửi email.
        PostLikeDTO postLikeDTO = PostLikeDTO.builder()
                .id(postLikeEntity.getId())
                .postId(postLikeEntity.getPostEntity().getId())
                .title(postLikeEntity.getPostEntity().getTitle())
                .slug(postLikeEntity.getPostEntity().getSlug())
                .userId(postLikeEntity.getUserEntity().getId())
                .username(postLikeEntity.getUserEntity().getUsername())
                .displayname(postLikeEntity.getUserEntity().getDisplayName())
                .likedAt(postLikeEntity.getCreatedDate())
                .build();
        return postLikeDTO;
    }

    @Transactional
    @Override
    public void deleteLikeByIdAndUsername(Long postId, String username) {
        postLikeRepository.deleteByUserEntity_UsernameAndPostEntity_Id(username, postId);
    }

    @Transactional
    @Override
    public PostCountDTO getLikeCount(Long postId) {
        PostEntity postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("No tfound"));
        long count = postLikeRepository.countByPostEntity_Id(postId);
        return PostCountDTO.builder()
                .title(postEntity.getTitle())
                .likeCount(count)
                .build();
    }
}
