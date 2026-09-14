package com.samosajunction.cart.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DualCartStoreTest {

    @Mock
    private RedisCartStore redisCartStore;

    @Mock
    private PostgresCartStore postgresCartStore;

    private DualCartStore dualCartStore;
    private UUID userId;
    private CartPayload payload;

    @BeforeEach
    void setUp() {
        dualCartStore = new DualCartStore(redisCartStore, postgresCartStore);
        userId = UUID.randomUUID();
        payload = new CartPayload(List.of(new CartItemData(UUID.randomUUID(), 2)));
    }

    @Test
    void loadPrefersRedis() {
        when(redisCartStore.load(userId)).thenReturn(payload);

        assertThat(dualCartStore.load(userId)).isEqualTo(payload);
    }

    @Test
    void loadFallsBackToPostgresWhenRedisFails() {
        when(redisCartStore.load(userId)).thenThrow(new QueryTimeoutException("redis down"));
        when(postgresCartStore.load(userId)).thenReturn(payload);

        assertThat(dualCartStore.load(userId)).isEqualTo(payload);
    }

    @Test
    void loadRestoresRedisFromPostgresOnMiss() {
        when(redisCartStore.load(userId)).thenReturn(null);
        when(postgresCartStore.load(userId)).thenReturn(payload);

        assertThat(dualCartStore.load(userId)).isEqualTo(payload);
        verify(redisCartStore).save(userId, payload);
    }

    @Test
    void saveWritesPostgresEvenIfRedisFails() {
        doThrow(new QueryTimeoutException("redis down")).when(redisCartStore).save(userId, payload);

        dualCartStore.save(userId, payload);

        verify(postgresCartStore).save(userId, payload);
    }
}
