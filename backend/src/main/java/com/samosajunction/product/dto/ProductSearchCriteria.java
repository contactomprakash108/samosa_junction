package com.samosajunction.product.dto;

import com.samosajunction.product.entity.SpiceLevel;

public record ProductSearchCriteria(
        String search,
        String category,
        SpiceLevel spiceLevel,
        Boolean available,
        String dietaryTag
) {
}
