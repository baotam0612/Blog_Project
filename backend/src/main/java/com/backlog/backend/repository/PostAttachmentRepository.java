package com.backlog.backend.repository;

import com.backlog.backend.entity.PostAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachmentEntity, Long> {
    List<PostAttachmentEntity> findAllByPost_IdOrderByCreatedAtDesc(Long postId);

    Optional<PostAttachmentEntity> findByIdAndPost_Id(Long id, Long postId);
}
