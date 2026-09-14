package com.samosajunction.cart.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartPayload(List<CartItemData> items) {

    public CartPayload {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static CartPayload empty() {
        return new CartPayload(List.of());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Optional<CartItemData> find(UUID productId) {
        return items.stream().filter(item -> item.productId().equals(productId)).findFirst();
    }
}
