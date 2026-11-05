package com.backlog.backend.repository;

import com.backlog.backend.entity.PostLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLikeEntity, Long> {

    boolean existsByUserEntity_UsernameAndPostEntity_Id(String username, Long postId);

    void deleteByUserEntity_UsernameAndPostEntity_Id(String username, Long postId);

    long countByPostEntity_Id(Long postId);
}
