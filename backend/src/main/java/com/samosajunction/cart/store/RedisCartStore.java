package com.samosajunction.cart.store;

import com.samosajunction.cart.config.CartProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisCartStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCartStore.class);

    private final RedisTemplate<String, CartPayload> redisTemplate;
    private final CartProperties properties;

    public RedisCartStore(RedisTemplate<String, CartPayload> cartRedisTemplate, CartProperties properties) {
        this.redisTemplate = cartRedisTemplate;
        this.properties = properties;
    }

    public CartPayload load(UUID userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    public void save(UUID userId, CartPayload payload) {
        redisTemplate.opsForValue().set(key(userId), payload, properties.ttl());
        log.debug("Cached cart {}", key(userId));
    }

    public void delete(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return properties.redisKeyPrefix() + userId;
    }
}
