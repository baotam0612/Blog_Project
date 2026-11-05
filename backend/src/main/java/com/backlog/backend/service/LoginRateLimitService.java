package com.backlog.backend.service;

public interface LoginRateLimitService {

    void checkAllowed(String username, String ipAddress);

    void recordFailure(String username, String ipAddress);

    void reset(String username, String ipAddress);
}
