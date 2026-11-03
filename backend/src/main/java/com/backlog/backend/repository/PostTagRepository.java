package com.backlog.backend.repository;

import com.backlog.backend.entity.PostTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTagEntity, Long> {
    List<PostTagEntity> findByPost_Id(Long postId);
    void deleteByPost_Id(Long postId);
}
