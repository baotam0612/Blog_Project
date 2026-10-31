package com.backlog.backend.config;

import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import com.backlog.backend.repository.InvalidatedTokenRepository;
import com.backlog.backend.service.TokenHash;
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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        httpSecurity.authorizeHttpRequests(request ->
                request.requestMatchers(HttpMethod.POST, "/api/v1/users", "/api/v1/auth/token", "/api/v1/auth/introspect").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                        .requestMatchers("/api/v1/users/**", "/api/v1/users").hasRole("ADMIN")
                        .anyRequest().denyAll());
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        httpSecurity.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(UserRepository userRepository,
                                                            UserRoleRepository userRoleRepository) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String userName = jwt.getSubject();
            if (userName == null || userName.isBlank()) {
                throw new BadCredentialsException("Token không có tài khoản");
            }
            var user = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new BadCredentialsException("Tài khoản không tồn tại"));
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa");
            }
            return userRoleRepository.findRoleCodesByUserName(user.getUsername()).stream()
                    .map(code -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + code))
                    .toList();
        });
        return converter;
    }


    @Bean
    JwtDecoder jwtDecoder(InvalidatedTokenRepository invalidatedTokenRepository){
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
            if (invalidatedTokenRepository.existsById(TokenHash.of(token))) {
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
