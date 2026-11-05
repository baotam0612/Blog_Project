package com.backlog.backend.service.Impl;

import com.backlog.backend.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final RedissonClient redissonClient;

    @Override
    public void blacklist(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            return;
        }

        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        redissonClient.getBucket(BLACKLIST_PREFIX + tokenId)
                .set("LOGGED_OUT", ttl);
    }

    @Override
    public boolean isBlacklisted(String tokenId) {
        return tokenId != null
                && !tokenId.isBlank()
                && redissonClient.getBucket(BLACKLIST_PREFIX + tokenId).isExists();
    }
}
