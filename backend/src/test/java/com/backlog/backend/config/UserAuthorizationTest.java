package com.backlog.backend.config;

import com.backlog.backend.controller.UserController;
import com.backlog.backend.model.dto.UserDTO;
import com.backlog.backend.service.TokenHash;
import com.backlog.backend.service.UserService;
import com.backlog.backend.service.TokenBlacklistService;
import com.backlog.backend.service.UserAuthCacheService;
import com.backlog.backend.model.dto.UserAuthCacheDTO;
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
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Base64;
import java.util.Date;

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
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserAuthCacheService userAuthCacheService;
    @MockitoBean ModelMapper modelMapper;

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
                .andExpect(status().isCreated());
    }

    @Test
    void bearerTokenUsesCachedRoleAndAccountStatus() throws Exception {
        when(userAuthCacheService.getUserAuth("alice"))
                .thenReturn(
                        UserAuthCacheDTO.builder().username("alice").active(true)
                                .roles(List.of("READER")).build(),
                        UserAuthCacheDTO.builder().username("alice").active(true)
                                .roles(List.of("ADMIN")).build()
                );
        String token = signedToken("alice");

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void disabledAccountCannotUseExistingToken() throws Exception {
        when(userAuthCacheService.getUserAuth("alice"))
                .thenReturn(UserAuthCacheDTO.builder().username("alice")
                        .active(false).roles(List.of()).build());

        mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + signedToken("alice")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggedOutTokenCannotBeUsed() throws Exception {
        String token = signedToken("alice");
        when(tokenBlacklistService.isBlacklisted(TokenHash.of(token))).thenReturn(true);

        mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
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
