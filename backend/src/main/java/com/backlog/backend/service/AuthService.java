package com.backlog.backend.service;

import com.backlog.backend.model.response.AuthResponse;
import com.backlog.backend.model.response.IntrospectResponse;
import com.backlog.backend.model.request.AuthRequest;
import com.backlog.backend.model.request.IntrospectRequest;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {

    AuthResponse authenticate(AuthRequest authRequest);
    String generateToken(String userName);
    IntrospectResponse authenticate(IntrospectRequest introspectRequest);
    void logout(Jwt jwt);
}
