package com.samosajunction.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean available,
        boolean productMissing,
        int stock
) {
}
