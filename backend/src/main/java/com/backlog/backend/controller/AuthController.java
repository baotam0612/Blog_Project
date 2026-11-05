package com.backlog.backend.controller;

import com.backlog.backend.Response.PayloadResponse;
import com.backlog.backend.model.response.AuthResponse;
import com.backlog.backend.model.request.AuthRequest;
import com.backlog.backend.model.request.IntrospectRequest;
import com.backlog.backend.service.AuthService;
import com.backlog.backend.service.LoginRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthService authService;
    LoginRateLimitService loginRateLimitService;

    // Login
    @PostMapping("/token")
    public PayloadResponse authenticate(
            @RequestBody AuthRequest authRequest,
            HttpServletRequest request
    ){

        String username = authRequest.getUsername();
        String ipAddress = request.getRemoteAddr();

        loginRateLimitService.checkAllowed(username, ipAddress);

        AuthResponse res;
        try {
            res = authService.authenticate(authRequest);
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            loginRateLimitService.recordFailure(username, ipAddress);
            throw ex;
        }

        loginRateLimitService.reset(username, ipAddress);
        return  new PayloadResponse(HttpStatus.OK, "Authenticated", res);
    }

    // Check token
    @PostMapping("/introspect")
    public PayloadResponse authenticate(@RequestBody IntrospectRequest introspectRequest){
        var res = authService.authenticate(introspectRequest);
        return new PayloadResponse(HttpStatus.OK, "introspected", res);
    }

    // Logout
    @PostMapping("/logout")
    public PayloadResponse logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt);
        return new PayloadResponse(HttpStatus.OK, "Logged out successfully");
    }
}
