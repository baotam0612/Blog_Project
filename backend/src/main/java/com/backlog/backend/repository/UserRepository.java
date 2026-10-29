package com.backlog.backend.repository;


import com.backlog.backend.entity.UserEntity;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {


    boolean existsByUsername(String userName);
    boolean existsByEmail(String email);
    Optional<UserEntity> findById(Long id);
    void deleteById(Long id);
    Optional<UserEntity> findByUsername(String userName);
    List<UserEntity> findByIsActive(Boolean isActive);
}
