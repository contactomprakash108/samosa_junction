package com.samosajunction.cart.store;

import java.util.UUID;

public interface CartStore {

    CartPayload load(UUID userId);

    void save(UUID userId, CartPayload payload);

    void delete(UUID userId);
}
