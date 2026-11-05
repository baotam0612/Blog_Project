package com.backlog.backend.service.Impl;

import com.backlog.backend.entity.UserEntity;
import com.backlog.backend.model.dto.UserAuthCacheDTO;
import com.backlog.backend.repository.UserRepository;
import com.backlog.backend.repository.UserRoleRepository;
import com.backlog.backend.service.UserAuthCacheService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UserAuthCacheServiceImpl implements UserAuthCacheService {

    // Prefix Key Redis "auth:user:" -> vd : auth:user:nguyenvana -> value {
//    "id" : 5,
//    "username" :  "nguyenvana",
//    "active" : true,
//    "roles" : "ADMIN"}
    private static final String USER_CACHE_PREFIX = "auth:user:";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    // thao tac Redis thong qua RedissonClient
    private final RedissonClient redissonClient;
    // Thoi han song cua cache
    private final Duration cacheTtl;

    public UserAuthCacheServiceImpl(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RedissonClient redissonClient,
            @Value("${app.redis.user-cache-ttl-minutes:15}") long cacheTtlMinutes
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.redissonClient = redissonClient;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    // dung mo hinh cache-aside : uu tien doc cache, neu khong co trong cache thi se tim trong db roi luu lai vao cache
    @Override
    public UserAuthCacheDTO getUserAuth(String username) {


//        RBucket<UserAuthCacheDTO> bucket1 = redissonClient.getBucket(cacheKey(username));
        RBucket<UserAuthCacheDTO> bucket = redissonClient.getBucket(cacheKey(username));   // tao bucket redis -> tra ve auth:username:nguyenvana

        // Lay ra value tu auth:user:nguyenvana
        UserAuthCacheDTO cachedUser = bucket.get();

        // Neu trong cache ton tai user
        if (cachedUser != null) {
            return cachedUser;
        }

        // Neu cache chua co user
        // Tim user trong DB
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));

        // Builder map sang UserAuthCacheDTO
        UserAuthCacheDTO userAuth = UserAuthCacheDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .active(user.getIsActive())
                .roles(userRoleRepository.findRoleCodesByUserName(user.getUsername()))
                .build();

        // Luu user vao Cache, set time ton tai trong cache
        bucket.set(userAuth, cacheTtl);

//        RBucket<UserAuthCacheDTO> bucket1 = redissonClient.getBucket(username);
//
//        UserAuthCacheDTO cacheUser1 = bucket1.get();
//
//        if(cachedUser != null){
//            return cachedUser;
//        }
//
//        UserEntity userEntity = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay user"));
//
//        UserAuthCacheDTO userAuthCacheDTO = UserAuthCacheDTO.builder()
//                .id(user.getId())
//                .username(user.getUsername())
//                .active(user.getIsActive())
//                .roles(userRoleRepository.findRoleCodesByUserName(username))
//                .build();
//
//        bucket.set(userAuthCacheDTO, cacheTtl);
        return userAuth;
    }

    @Override
    public void deleteUserAuth(String username) {
        if (username != null && !username.isBlank()) {
            redissonClient.getBucket(cacheKey(username)).delete();
        }
    }

    // Chuan hoa auth:user:nguyenvana
    private String cacheKey(String username) {
        return USER_CACHE_PREFIX + username.trim().toLowerCase();
    }
}
