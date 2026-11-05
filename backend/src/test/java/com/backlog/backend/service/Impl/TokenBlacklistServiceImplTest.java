package com.backlog.backend.service.Impl;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceImplTest {

    private final RedissonClient redisson = mock(RedissonClient.class);

    @SuppressWarnings("unchecked")
    private final RBucket<String> bucket = mock(RBucket.class);

    private final TokenBlacklistServiceImpl service = new TokenBlacklistServiceImpl(redisson);

    @Test
    void blacklistUsesTokenIdAndRemainingJwtLifetime() {
        when(redisson.<String>getBucket("auth:blacklist:jwt-id")).thenReturn(bucket);

        service.blacklist("jwt-id", Instant.now().plusSeconds(300));

        verify(bucket).set(org.mockito.ArgumentMatchers.eq("LOGGED_OUT"), any(Duration.class));
    }

    @Test
    void detectsBlacklistedToken() {
        when(redisson.<String>getBucket("auth:blacklist:jwt-id")).thenReturn(bucket);
        when(bucket.isExists()).thenReturn(true);

        assertTrue(service.isBlacklisted("jwt-id"));
    }
}
