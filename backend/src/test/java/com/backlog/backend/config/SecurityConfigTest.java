package com.backlog.backend.config;

import com.backlog.backend.model.dto.UserAuthCacheDTO;
import com.backlog.backend.service.UserAuthCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {
    private final UserAuthCacheService userAuthCacheService = mock(UserAuthCacheService.class);
    private final SecurityConfig config = new SecurityConfig();
    private final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("alice")
            .build();

    @Test
    void resolvesCurrentRolesFromCache() {
        when(userAuthCacheService.getUserAuth("alice"))
                .thenReturn(
                        UserAuthCacheDTO.builder().username("alice").active(true)
                                .roles(List.of("READER")).build(),
                        UserAuthCacheDTO.builder().username("alice").active(true)
                                .roles(List.of("ADMIN")).build()
                );

        var converter = config.jwtAuthenticationConverter(userAuthCacheService);
        assertTrue(converter.convert(jwt).getAuthorities().stream()
                .anyMatch(authority -> "ROLE_READER".equals(authority.getAuthority())));
        assertTrue(converter.convert(jwt).getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void rejectsDisabledAccount() {
        when(userAuthCacheService.getUserAuth("alice"))
                .thenReturn(UserAuthCacheDTO.builder()
                        .username("alice").active(false).roles(List.of()).build());

        var converter = config.jwtAuthenticationConverter(userAuthCacheService);
        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }
}
