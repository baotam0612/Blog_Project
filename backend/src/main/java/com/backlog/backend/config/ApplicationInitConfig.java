package com.backlog.backend.config;

import com.backlog.backend.entity.RoleEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.entity.UserRoleEntity;
import com.backlog.backend.repository.RoleRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationInitConfig {
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository,
                                        RoleRepository roleRepository,
                                        UserRoleRepository userRoleRepository,
                                        @Value("${app.admin.username:}") String adminUsername,
                                        @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            RoleEntity adminRole = roleRepository.findByCode("ADMIN")
                    .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                            .code("ADMIN").name("Administrator").build()));
            roleRepository.findByCode("READER")
                    .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                            .code("READER").name("Reader").build()));

            if (adminUsername.isBlank() && adminPassword.isBlank()) {
                return;
            }
            if (adminUsername.isBlank() || adminPassword.isBlank()) {
                throw new IllegalStateException("Cần cấu hình cả app.admin.username và app.admin.password");
            }

            UserEntity admin = userRepository.findByUsername(adminUsername)
                    .orElseGet(() -> userRepository.save(UserEntity.builder()
                            .username(adminUsername)
                            .password(passwordEncoder.encode(adminPassword))
                            .isActive(true)
                            .build()));
            if (!userRoleRepository.findRoleCodesByUserName(adminUsername).contains("ADMIN")) {
                userRoleRepository.save(new UserRoleEntity(adminRole, admin));
            }
        };
    }
}
