package com.backlog.backend.config;

import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final SecurityConfig config = new SecurityConfig();
    private final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("alice")
            .build();

    @Test
    void resolvesCurrentRolesFromDatabase() {
        UserEntity user = UserEntity.builder().username("alice").isActive(true).build();
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRoles.findRoleCodesByUserName("alice"))
                .thenReturn(List.of("READER"), List.of("ADMIN"));

        var converter = config.jwtAuthenticationConverter(users, userRoles);
        assertTrue(converter.convert(jwt).getAuthorities().stream()
                .anyMatch(authority -> "ROLE_READER".equals(authority.getAuthority())));
        assertTrue(converter.convert(jwt).getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void rejectsDisabledAccount() {
        when(users.findByUsername("alice"))
                .thenReturn(Optional.of(UserEntity.builder().username("alice").isActive(false).build()));

        var converter = config.jwtAuthenticationConverter(users, userRoles);
        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }
}
