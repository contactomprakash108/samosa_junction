package com.samosajunction.cart.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DualCartStore implements CartStore {

    private static final Logger log = LoggerFactory.getLogger(DualCartStore.class);

    private final RedisCartStore redisCartStore;
    private final PostgresCartStore postgresCartStore;

    public DualCartStore(RedisCartStore redisCartStore, PostgresCartStore postgresCartStore) {
        this.redisCartStore = redisCartStore;
        this.postgresCartStore = postgresCartStore;
    }

    @Override
    public CartPayload load(UUID userId) {
        try {
            CartPayload cached = redisCartStore.load(userId);
            if (cached != null) {
                return cached;
            }
        } catch (RuntimeException ex) {
            log.warn("Redis cart read failed for user {}; using PostgreSQL", userId, ex);
            CartPayload durable = postgresCartStore.load(userId);
            return durable == null ? CartPayload.empty() : durable;
        }

        CartPayload durable = postgresCartStore.load(userId);
        if (durable == null) {
            return CartPayload.empty();
        }
        try {
            redisCartStore.save(userId, durable);
        } catch (RuntimeException ex) {
            log.warn("Redis cart restore failed for user {}", userId, ex);
        }
        return durable;
    }

    @Override
    public void save(UUID userId, CartPayload payload) {
        postgresCartStore.save(userId, payload);
        try {
            redisCartStore.save(userId, payload);
        } catch (RuntimeException ex) {
            log.warn("Redis cart write failed for user {}; PostgreSQL has the latest cart", userId, ex);
        }
    }

    @Override
    public void delete(UUID userId) {
        postgresCartStore.delete(userId);
        try {
            redisCartStore.delete(userId);
        } catch (RuntimeException ex) {
            log.warn("Redis cart delete failed for user {}; PostgreSQL row removed", userId, ex);
        }
    }
}
