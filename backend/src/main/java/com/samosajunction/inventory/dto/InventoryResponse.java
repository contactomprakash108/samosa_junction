package com.samosajunction.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        int quantity,
        long version,
        Instant updatedAt
) {
}
