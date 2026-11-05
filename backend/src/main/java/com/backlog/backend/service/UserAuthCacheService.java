package com.backlog.backend.service;

import com.backlog.backend.model.dto.UserAuthCacheDTO;

public interface UserAuthCacheService {
    UserAuthCacheDTO getUserAuth(String username);

    void deleteUserAuth(String username);
}
