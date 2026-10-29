package com.backlog.backend.repository;

import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {
    List<UserRoleEntity>  findAllByUser(UserEntity user);

    @Query("select ur.role.code from UserRoleEntity ur where ur.user.username = :userName")
    List<String> findRoleCodesByUserName(@Param("userName") String userName);
}
