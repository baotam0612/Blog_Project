package com.backlog.backend.service.Impl;


import com.backlog.backend.model.response.AuthResponse;
import com.backlog.backend.model.response.IntrospectResponse;
import com.backlog.backend.model.request.AuthRequest;
import com.backlog.backend.model.request.IntrospectRequest;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.service.AuthService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthServiceImpl implements AuthService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;
    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        var user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Sai ten nguoi dung"));
        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new DisabledException("Tài khoản đã bị vô hiệu hóa");
        boolean authenticated = passwordEncoder.matches(authRequest.getPassword(), user.getPassword());
        if(!authenticated)
            throw new BadCredentialsException("Sai mat khau");

        var token = generateToken(authRequest.getUsername());

        return AuthResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    @Override
    public IntrospectResponse authenticate(IntrospectRequest introspectRequest) {
        try {
            byte[] secret = Base64.getDecoder().decode(SIGNER_KEY);
            JWSVerifier verifier = new MACVerifier(secret);
            SignedJWT jwt = SignedJWT.parse(introspectRequest.getToken());

            boolean verified = jwt.verify(verifier);
            Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
            String userName = jwt.getJWTClaimsSet().getSubject();
            boolean activeUser = userName != null && userRepository.findByUsername(userName)
                    .map(user -> Boolean.TRUE.equals(user.getIsActive()))
                    .orElse(false);

            return IntrospectResponse.builder()
                    .valid(verified
                            && expirationTime != null
                            && expirationTime.after(new Date())
                            && activeUser)
                    .build();
        } catch (JOSEException | ParseException | IllegalArgumentException e) {
            return IntrospectResponse.builder().valid(false).build();
        }


    }

    @Override
    public String generateToken(String userName) {

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userName)
                .issuer("blog.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .build();
        Payload payload =  new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(Base64.getDecoder().decode(SIGNER_KEY)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token");
            throw new RuntimeException(e);
        }



    }
}
