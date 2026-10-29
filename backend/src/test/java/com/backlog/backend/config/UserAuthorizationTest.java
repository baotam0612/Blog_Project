package com.backlog.backend.config;

import com.backlog.backend.controller.UserController;
import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.model.dto.UserDTO;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import com.backlog.backend.service.UserService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(value = UserController.class, properties = "jwt.signerKey=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Import(SecurityConfig.class)
class UserAuthorizationTest {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    @Autowired MockMvc mvc;
    @MockitoBean UserService userService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean UserRoleRepository userRoleRepository;

    @Test
    void anonymousCannotListUsers() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "READER")
    void readerCannotManageUsers() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        when(userService.getAllUser()).thenReturn(List.of());
        mvc.perform(get("/api/v1/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "READER")
    void readerCanViewOwnProfile() throws Exception {
        UserDTO profile = new UserDTO();
        profile.setUsername("user");
        when(userService.getCurrentUser("user")).thenReturn(profile);
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("user"));
    }

    @Test
    void registrationIsPublic() throws Exception {
        mvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void bearerTokenUsesDatabaseRoleAndAccountStatus() throws Exception {
        UserEntity user = UserEntity.builder().username("alice").isActive(true).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRoleRepository.findRoleCodesByUserName("alice"))
                .thenReturn(List.of("READER"), List.of("ADMIN"));
        String token = signedToken("alice");

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void disabledAccountCannotUseExistingToken() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(UserEntity.builder().username("alice").isActive(false).build()));

        mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + signedToken("alice")))
                .andExpect(status().isUnauthorized());
    }

    private String signedToken(String subject) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject(subject)
                        .issueTime(new Date())
                        .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                        .build());
        jwt.sign(new MACSigner(Base64.getDecoder().decode(TEST_SECRET)));
        return jwt.serialize();
    }
}
