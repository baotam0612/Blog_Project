package com.backlog.backend.service.Impl;

import com.backlog.backend.service.LoginRateLimitService;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Locale;

@Service
public class LoginRateLimitServiceImpl implements LoginRateLimitService {

    private static final String LOGIN_RATE_PREFIX = "rate:login:";

    private final RedissonClient redissonClient;
    private final long userLimit;
    private final long ipLimit;
    private final Duration interval;

    public LoginRateLimitServiceImpl(
            RedissonClient redissonClient,
            @Value("${app.redis.login-rate-limit.per-user:5}") long userLimit,
            @Value("${app.redis.login-rate-limit.per-ip:20}") long ipLimit,
            @Value("${app.redis.login-rate-limit.interval-seconds:60}") long intervalSeconds
    ) {
        this.redissonClient = redissonClient;
        this.userLimit = userLimit;
        this.ipLimit = ipLimit;
        this.interval = Duration.ofSeconds(intervalSeconds);
    }

    @Override
    public void checkAllowed(String username, String ipAddress) {
        if (failureCount(userKey(username)) >= userLimit
                || failureCount(ipKey(ipAddress)) >= ipLimit) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đăng nhập quá nhiều lần, vui lòng thử lại sau"
            );
        }
    }

    @Override
    public void recordFailure(String username, String ipAddress) {
        incrementWithTtl(userKey(username));
        incrementWithTtl(ipKey(ipAddress));
    }

    @Override
    public void reset(String username, String ipAddress) {
        redissonClient.getAtomicLong(userKey(username)).delete();
        redissonClient.getAtomicLong(ipKey(ipAddress)).delete();
    }

    private long failureCount(String key) {
        return redissonClient.getAtomicLong(key).get();
    }

    private void incrementWithTtl(String key) {
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        if (counter.incrementAndGet() == 1) {
            counter.expire(interval);
        }
    }

    private String userKey(String username) {
        String value = username == null
                ? "unknown"
                : username.trim().toLowerCase(Locale.ROOT);
        return LOGIN_RATE_PREFIX + "user:" + value;
    }

    private String ipKey(String ipAddress) {
        String value = ipAddress == null || ipAddress.isBlank()
                ? "unknown"
                : ipAddress;
        return LOGIN_RATE_PREFIX + "ip:" + value;
    }
}
