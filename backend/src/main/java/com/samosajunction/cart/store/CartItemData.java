package com.samosajunction.cart.store;

import java.util.UUID;

public record CartItemData(UUID productId, int quantity) {
}
