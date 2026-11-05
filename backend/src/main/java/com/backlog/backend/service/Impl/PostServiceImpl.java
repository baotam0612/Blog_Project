package com.backlog.backend.service.Impl;

import com.backlog.backend.Specification.PostSpecification;
import com.backlog.backend.entity.*;
import com.backlog.backend.exception.ForbiddenException;
import com.backlog.backend.exception.ResourceNotFoundException;
import com.backlog.backend.model.dto.PostDTO;
import com.backlog.backend.model.request.PostRequest;
import com.backlog.backend.model.request.UpdateStatusRequest;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.repository.*;
import com.backlog.backend.service.PostAttachmentService;
import com.backlog.backend.service.PostService;
import com.backlog.backend.service.PostTagService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.ResourceClosedException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostServiceImpl implements PostService {
    PostRepository postRepository;
    ModelMapper modelMapper;
    CategoryRepository categoryRepository;
    PostTagRepository postTagRepository;
    TagRepository tagRepository;
    UserRepository userRepository;
    PostAttachmentService postAttachmentService;



    // Get All Posts
    @Override
    @Transactional()
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        Page<PostEntity> listPost = postRepository.findAll(pageable);
        return listPost.map(it -> {
            List<PostTagEntity> listPostTag = it.getListPost();
            List<String> listTags = new ArrayList<>();
            for(PostTagEntity item : listPostTag){
                listTags.add(item.getTag().getName());
            }
            return PostResponse.builder()
                    .title(it.getTitle())
                    .slug(it.getSlug())
                    .excerpt(it.getExcerpt())
                    .coverImageUrl(it.getCoverImageUrl())
                    .content(it.getContent())
                    .status(it.getStatus())
                    .createdAt(it.getCreatedAt())
                    .updatedAt(it.getUpdatedAt())
                    .publishedAt(it.getPublishedAt())
                    .username(it.getUserPost().getDisplayName())
                    .categoryName(it.getCategory().getName())
                    .tagNames(listTags)
                    .build();
        });
    }

    @Override
    @Transactional
    public PostDTO createPost(PostRequest postRequest, String username) {
        boolean checkPostSlug = postRepository.existsBySlug(postRequest.getSlug());
        if(checkPostSlug){
            throw new RuntimeException("Bai viet da xuat hien");
        }
        PostEntity postEntity = PostEntity.builder()
                .title(postRequest.getTitle())
                .slug(postRequest.getSlug())
                .excerpt(postRequest.getExcerpt())
                .coverImageUrl(postRequest.getCoverImageUrl())
                .content(postRequest.getContent())
                .status(postRequest.getStatus())
                .createdAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .build();

        CategoryEntity category = categoryRepository.findById(postRequest.getCategoryId())
                .orElseThrow(() -> new ResourceClosedException("Khong tim thay category de luu post"));
        postEntity.setCategory(category);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        // Luu post
        postEntity.setUserPost(user);
        postRepository.save(postEntity);
        List<Long> tagIds = postRequest.getTagIds();
        List<String> tagNames = new ArrayList<>();
        for(Long id : tagIds){
            TagEntity tagEntity = tagRepository.findById(id)
                    .orElseThrow(() -> new ResourceClosedException("Khong tim thay Tag trong create Post"));
            tagNames.add(tagEntity.getName());
            PostTagEntity postTagEntity = new PostTagEntity();
            postTagEntity.setTag(tagEntity);
            postTagEntity.setPost(postEntity);
            postTagRepository.save(postTagEntity);
        }

        return PostDTO.builder()
                .id(postEntity.getId())
                .title(postEntity.getTitle())
                .slug(postEntity.getSlug())
                .excerpt(postEntity.getExcerpt())
                .coverImageUrl(postEntity.getCoverImageUrl())
                .content(postEntity.getContent())
                .status(postEntity.getStatus())
                .publishedAt(LocalDateTime.now())
                .authorId(postEntity.getUserPost().getId())
                .authorUsername(postEntity.getUserPost().getUsername())
                .authorDisplayName(postEntity.getUserPost().getDisplayName())
                .categoryId(postEntity.getCategory().getId())
                .categoryName(postEntity.getCategory().getName())
                .tagIds(postRequest.getTagIds())
                .tagNames(tagNames)
                .build();
    }

    // Update Post Status
    @Transactional
    @Override
    public PostDTO updatePostStatus(Long id, UpdateStatusRequest status, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        PostEntity postEntity = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found to update status"));

        if (!Objects.equals(postEntity.getUserPost().getId(), user.getId())) {
            throw new ForbiddenException("User does not own this post");
        }

        postEntity.setStatus(status.getStatus());

        postRepository.save(postEntity);
        return PostDTO.builder()
                .id(postEntity.getId())
                .title(postEntity.getTitle())
                .slug(postEntity.getSlug())
                .excerpt(postEntity.getExcerpt())
                .coverImageUrl(postEntity.getCoverImageUrl())
                .content(postEntity.getContent())
                .status(postEntity.getStatus())
                .createdAt(postEntity.getCreatedAt())
                .updatedAt(postEntity.getUpdatedAt())
                .publishedAt(postEntity.getPublishedAt())
                .authorId(postEntity.getUserPost().getId())
                .authorUsername(postEntity.getUserPost().getUsername())
                .authorDisplayName(postEntity.getUserPost().getDisplayName())
                .categoryId(postEntity.getCategory().getId())
                .categoryName(postEntity.getCategory().getName())
                .build();
    }

    // Update Post Info
    @Transactional
    @Override
    public PostDTO updatePost(Long id, PostRequest postRequest, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        PostEntity postEntity = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found to update post"));
        if(!Objects.equals(postEntity.getUserPost().getId(), user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"User Forbidden");

        CategoryEntity category = categoryRepository.findById(postRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found to update post"));

     //   List<PostTagEntity> listPostTag = postTagRepository.findByPost_Id(id);
        postTagRepository.deleteByPost_Id(id);
        postTagRepository.flush();

        postEntity.setTitle(postRequest.getTitle());
        postEntity.setSlug(postRequest.getSlug());
        postEntity.setExcerpt(postRequest.getExcerpt());
        postEntity.setCoverImageUrl(postRequest.getCoverImageUrl());
        postEntity.setContent(postRequest.getContent());
        postEntity.setStatus(postRequest.getStatus());
        postEntity.setCategory(category);

        UserEntity author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
        // Luu post
        postEntity.setUserPost(author);
        postRepository.save(postEntity);
        List<String> tagNames = new ArrayList<>();
        for(Long item : postRequest.getTagIds()){
            TagEntity tagEntity = tagRepository.findById(item)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found to update post"));
            tagNames.add(tagEntity.getName());
            PostTagEntity postTagEntity = new PostTagEntity();
            postTagEntity.setPost(postEntity);
            postTagEntity.setTag(tagEntity);
            postTagRepository.save(postTagEntity);
        }

        return PostDTO.builder()
                .id(postEntity.getId())
                .title(postEntity.getTitle())
                .slug(postEntity.getSlug())
                .excerpt(postEntity.getExcerpt())
                .coverImageUrl(postEntity.getCoverImageUrl())
                .content(postEntity.getContent())
                .status(postEntity.getStatus())
                .createdAt(postEntity.getCreatedAt())
                .updatedAt(postEntity.getUpdatedAt())
                .publishedAt(postEntity.getPublishedAt())
                .authorId(postEntity.getUserPost().getId())
                .authorUsername(postEntity.getUserPost().getUsername())
                .authorDisplayName(postEntity.getUserPost().getDisplayName())
                .categoryId(postEntity.getCategory().getId())
                .categoryName(postEntity.getCategory().getName())
                .tagIds(postRequest.getTagIds())
                .tagNames(tagNames)
                .build();
    }

    // Delete Post
    @Transactional
    @Override
    public void deletePostById(Long id, String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        PostEntity postEntity = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found to update post"));
        if(!Objects.equals(postEntity.getUserPost().getId(), user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"User Forbidden");
        postAttachmentService.deleteAllByPostId(id);
        postRepository.deleteById(id);
    }

    // Search Post

    @Transactional
    @Override
    public Page<PostDTO> searchPostByTitleAndAuthor(Pageable pageable, String search) {
        Page<PostEntity> postPage = postRepository.findAll( PostSpecification.search(search),pageable);
        return postPage.map(postEntity -> {
            List<Long> tagIds = new ArrayList<>();
            List<String> tagNames = new ArrayList<>();

            for (PostTagEntity postTag : postEntity.getListPost()) {
                tagIds.add(postTag.getTag().getId());
                tagNames.add(postTag.getTag().getName());
            }

            return PostDTO.builder()
                    .id(postEntity.getId())
                    .title(postEntity.getTitle())
                    .slug(postEntity.getSlug())
                    .excerpt(postEntity.getExcerpt())
                    .coverImageUrl(postEntity.getCoverImageUrl())
                    .content(postEntity.getContent())
                    .status(postEntity.getStatus())
                    .createdAt(postEntity.getCreatedAt())
                    .updatedAt(postEntity.getUpdatedAt())
                    .publishedAt(postEntity.getPublishedAt())
                    .authorId(postEntity.getUserPost().getId())
                    .authorUsername(postEntity.getUserPost().getUsername())
                    .authorDisplayName(postEntity.getUserPost().getDisplayName())
                    .categoryId(postEntity.getCategory().getId())
                    .categoryName(postEntity.getCategory().getName())
                    .tagIds(tagIds)
                    .tagNames(tagNames)
                    .build();
        });
    }
}
