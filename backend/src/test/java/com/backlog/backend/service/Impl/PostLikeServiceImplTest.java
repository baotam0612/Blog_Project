package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.PostEntity;
import com.backlog.backend.entity.PostLikeEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.event.PostLikedEvent;
import com.backlog.backend.repository.PostLikeRepository;
import com.backlog.backend.repository.PostRepository;
import com.backlog.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostLikeServiceImplTest {

    private final PostRepository posts = mock(PostRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PostLikeRepository likes = mock(PostLikeRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PostLikeServiceImpl service =
            new PostLikeServiceImpl(posts, users, likes, events);

    @Test
    void likeByAnotherUserPublishesEmailEvent() {
        UserEntity owner = UserEntity.builder()
                .id(1L).username("owner").email("owner@example.com").build();
        UserEntity liker = UserEntity.builder()
                .id(2L).username("alice").displayName("Alice").build();
        PostEntity post = PostEntity.builder()
                .id(10L).title("Spring Redis").slug("spring-redis").userPost(owner).build();
        when(posts.findById(10L)).thenReturn(Optional.of(post));
        when(users.findByUsername("alice")).thenReturn(Optional.of(liker));
        when(likes.existsByUserEntity_UsernameAndPostEntity_Id("alice", 10L)).thenReturn(false);
        when(likes.saveAndFlush(any(PostLikeEntity.class))).thenAnswer(invocation -> {
            PostLikeEntity saved = invocation.getArgument(0);
            saved.setId(5L);
            saved.setCreatedDate(LocalDateTime.of(2026, 11, 4, 10, 0));
            return saved;
        });

        service.doLike(10L, "alice");

        ArgumentCaptor<PostLikedEvent> event = ArgumentCaptor.forClass(PostLikedEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("owner@example.com", event.getValue().getOwnerEmail());
        assertEquals("alice", event.getValue().getLikedByUsername());
    }

    @Test
    void likingOwnPostDoesNotPublishEmailEvent() {
        UserEntity owner = UserEntity.builder().id(1L).username("owner").build();
        PostEntity post = PostEntity.builder().id(10L).userPost(owner).build();
        when(posts.findById(10L)).thenReturn(Optional.of(post));
        when(users.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(likes.saveAndFlush(any(PostLikeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.doLike(10L, "owner");

        verify(events, never()).publishEvent(any());
    }
}
