package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.InvalidatedTokenEntity;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.model.request.IntrospectRequest;
import com.backlog.backend.repository.InvalidatedTokenRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.service.TokenHash;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class AuthServiceImplTest {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final UserRepository users = mock(UserRepository.class);
    private final InvalidatedTokenRepository invalidatedTokens = mock(InvalidatedTokenRepository.class);
    private final AuthServiceImpl service = new AuthServiceImpl(
            users, mock(PasswordEncoder.class), invalidatedTokens);

    @Test
    void separateLoginsReceiveDifferentTokens() {
        ReflectionTestUtils.setField(service, "SIGNER_KEY", TEST_SECRET);

        String first = service.generateToken("alice");
        String second = service.generateToken("alice");

        assertFalse(first.equals(second));
    }

    @Test
    void logoutStoresHashAndExpiry() {
        Instant expiry = Instant.now().plusSeconds(3600);
        Jwt jwt = Jwt.withTokenValue("signed.jwt.token")
                .header("alg", "HS256")
                .subject("alice")
                .expiresAt(expiry)
                .build();

        service.logout(jwt);

        ArgumentCaptor<InvalidatedTokenEntity> saved = ArgumentCaptor.forClass(InvalidatedTokenEntity.class);
        verify(invalidatedTokens).save(saved.capture());
        assertEquals(TokenHash.of(jwt.getTokenValue()), saved.getValue().getTokenHash());
        assertEquals(expiry, saved.getValue().getExpiresAt());
    }

    @Test
    void introspectRejectsLoggedOutToken() throws Exception {
        ReflectionTestUtils.setField(service, "SIGNER_KEY", TEST_SECRET);
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject("alice")
                        .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                        .build());
        jwt.sign(new MACSigner(Base64.getDecoder().decode(TEST_SECRET)));
        String token = jwt.serialize();
        when(users.findByUsername("alice"))
                .thenReturn(Optional.of(UserEntity.builder().username("alice").isActive(true).build()));
        when(invalidatedTokens.existsById(eq(TokenHash.of(token))))
                .thenReturn(false, true);

        assertTrue(service.authenticate(new IntrospectRequest(token)).isValid());
        assertFalse(service.authenticate(new IntrospectRequest(token)).isValid());
    }
}
