package com.backlog.backend.controller;

import com.backlog.backend.config.SecurityConfig;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.repository.InvalidatedTokenRepository;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import com.backlog.backend.service.AuthService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, properties = "jwt.signerKey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Import(SecurityConfig.class)
class AuthControllerTest {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean UserRoleRepository userRoleRepository;
    @MockitoBean InvalidatedTokenRepository invalidatedTokenRepository;

    @Test
    void logoutRequiresBearerToken() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutPassesAuthenticatedJwtToService() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(UserEntity.builder().username("alice").isActive(true).build()));
        String token = signedToken();

        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        ArgumentCaptor<Jwt> jwt = ArgumentCaptor.forClass(Jwt.class);
        verify(authService).logout(jwt.capture());
        assertEquals(token, jwt.getValue().getTokenValue());
    }

    private String signedToken() throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject("alice")
                        .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                        .build());
        jwt.sign(new MACSigner(Base64.getDecoder().decode(TEST_SECRET)));
        return jwt.serialize();
    }
}
