package com.backlog.backend.config;

import com.backlog.backend.model.dto.UserAuthCacheDTO;
import com.backlog.backend.service.TokenHash;
import com.backlog.backend.service.TokenBlacklistService;
import com.backlog.backend.service.UserAuthCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity

public class SecurityConfig {

    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    // Chứa toàn bộ chuỗi filter và rule URL
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        httpSecurity.authorizeHttpRequests(request ->
                request.requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/auth/token", "/api/v1/auth/introspect").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .requestMatchers("/api/v1/posts/**").authenticated()
                        .requestMatchers("/api/v1/users/**", "/api/v1/users").hasRole("ADMIN")
                        .anyRequest().denyAll());

        // Spring lay Bearer token
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        httpSecurity.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    // Lấy username từ Jwt, lấy user/role từ Redis hoặc DB, tạo authority như ROLE_ADMIN
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(
            UserAuthCacheService userAuthCacheService
    ) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // lay subject trong token -> username
            String username = jwt.getSubject();

            if (username == null || username.isBlank()) {
                throw new BadCredentialsException("Token không có tài khoản");
            }

            UserAuthCacheDTO user = userAuthCacheService.getUserAuth(username);

            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa");
            }

            // Doi "ADMIN" -> "ROLE_ADMIN"
            return user.getRoles().stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return converter;
    }



    //Nhận chuỗi JWT, kiểm tra chữ ký, expiry, blacklist; rồi trả về object Jwt
    @Bean
    JwtDecoder jwtDecoder(TokenBlacklistService tokenBlacklistService){
        // gồm 2 thông số secret va algo
        byte[] secret = Base64.getDecoder().decode(SIGNER_KEY);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret, "HS256");
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        return token -> {
            var jwt = decoder.decode(token);
            if (jwt.getExpiresAt() == null) {
                throw new BadJwtException("Token không có hạn sử dụng");
            }
            String tokenId = jwt.getId();
            if (tokenId == null || tokenId.isBlank()) {
                tokenId = TokenHash.of(token);
            }
            if (tokenBlacklistService.isBlacklisted(tokenId)) {
                throw new BadJwtException("Token đã được đăng xuất");
            }
            return jwt;
        };
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

}
