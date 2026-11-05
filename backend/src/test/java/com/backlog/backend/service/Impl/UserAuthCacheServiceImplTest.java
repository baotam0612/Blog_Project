package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.model.dto.UserAuthCacheDTO;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAuthCacheServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private final RedissonClient redisson = mock(RedissonClient.class);

    @SuppressWarnings("unchecked")
    private final RBucket<UserAuthCacheDTO> bucket = mock(RBucket.class);

    private final UserAuthCacheServiceImpl service =
            new UserAuthCacheServiceImpl(users, userRoles, redisson, 15);

    @Test
    void cacheHitDoesNotQueryDatabase() {
        UserAuthCacheDTO cached = UserAuthCacheDTO.builder()
                .id(1L).username("alice").active(true).roles(List.of("READER")).build();
        when(redisson.<UserAuthCacheDTO>getBucket("auth:user:alice")).thenReturn(bucket);
        when(bucket.get()).thenReturn(cached);

        UserAuthCacheDTO result = service.getUserAuth("Alice");

        assertEquals(cached, result);
        verify(users, never()).findByUsername("Alice");
    }

    @Test
    void cacheMissLoadsDatabaseAndStoresWithTtl() {
        UserEntity user = UserEntity.builder()
                .id(1L).username("alice").isActive(true).build();
        when(redisson.<UserAuthCacheDTO>getBucket("auth:user:alice")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRoles.findRoleCodesByUserName("alice")).thenReturn(List.of("READER"));

        UserAuthCacheDTO result = service.getUserAuth("alice");

        assertEquals(List.of("READER"), result.getRoles());
        verify(bucket).set(result, Duration.ofMinutes(15));
    }
}
