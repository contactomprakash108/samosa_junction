package com.samosajunction.staff.dto;

import java.util.UUID;

public record StaffInventoryItemResponse(
        UUID productId,
        String name,
        int quantity,
        boolean available,
        boolean lowStock,
        boolean soldOut
) {
}
