package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.PostEntity;
import com.backlog.backend.entity.PostTagEntity;
import com.backlog.backend.entity.TagEntity;
import com.backlog.backend.model.response.PostResponse;
import com.backlog.backend.repository.PostRepository;
import com.backlog.backend.service.PostService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostServiceImpl implements PostService {
    PostRepository postRepository;



    @Override
    @Transactional()
    public List<PostResponse> getAllPosts() {
        List<PostEntity> listPost = postRepository.findAll();
        List<PostResponse> res = new ArrayList<>();
        for(PostEntity it : listPost){
            List<PostTagEntity> listPostTag = it.getListPost();
            List<String> listTags = new ArrayList<>();
            for(PostTagEntity item : listPostTag){
                listTags.add(item.getTag().getName());
            }
            res.add(PostResponse.builder()
                    .title(it.getTitle())
                    .slug(it.getSlug())
                    .excerpt(it.getExcerpt())
                    .coverImageUrl(it.getCoverImageUrl())
                    .content(it.getContent())
                    .status(it.getStatus())
                    .createdAt(it.getCreatedAt())
                    .updatedAt(it.getUpdatedAt())
                    .pushlishedAt(it.getPublishedAt())
                    .username(it.getUserPost().getDisplayName())
                    .categoryName(it.getCategory().getName())
                    .tagname(listTags)
                    .build());
        }
        return res;
    }
}
