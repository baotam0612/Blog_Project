package com.backlog.backend.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users",uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"), @UniqueConstraint(columnNames = "email")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @Column(name="username")
     String username;

    @Column(name="email")
     String email;

    @Column(name = "password_hash")
     String password;

    @Column(name = "display_name")
     String displayName;

    @Column(name = "is_active")
     Boolean isActive;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
     List<UserRoleEntity> listUserRole;

    @OneToMany(mappedBy = "userPost", fetch = FetchType.LAZY)
    List<PostEntity> listPost ;



}
