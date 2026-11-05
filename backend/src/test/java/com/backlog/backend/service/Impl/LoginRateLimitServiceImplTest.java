package com.backlog.backend.service.Impl;

import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginRateLimitServiceImplTest {

    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RAtomicLong userCounter = mock(RAtomicLong.class);
    private final RAtomicLong ipCounter = mock(RAtomicLong.class);
    private final LoginRateLimitServiceImpl service =
            new LoginRateLimitServiceImpl(redisson, 5, 20, 60);

    @Test
    void allowsLoginWhenBothLimitsHavePermits() {
        configureCounters(0, 0);

        assertDoesNotThrow(() -> service.checkAllowed("Alice", "127.0.0.1"));
    }

    @Test
    void rejectsLoginWhenUserLimitIsExhausted() {
        configureCounters(5, 0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.checkAllowed("Alice", "127.0.0.1")
        );
        assertEquals(429, exception.getStatusCode().value());
    }

    private void configureCounters(long userFailures, long ipFailures) {
        when(redisson.getAtomicLong("rate:login:user:alice")).thenReturn(userCounter);
        when(redisson.getAtomicLong("rate:login:ip:127.0.0.1")).thenReturn(ipCounter);
        when(userCounter.get()).thenReturn(userFailures);
        when(ipCounter.get()).thenReturn(ipFailures);
    }
}
