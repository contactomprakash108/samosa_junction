package com.samosajunction.product.dto;

import com.samosajunction.product.entity.Product;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.support.InrMoney;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        String category,
        BigDecimal price,
        Set<String> ingredients,
        int calories,
        BigDecimal protein,
        SpiceLevel spiceLevel,
        Set<String> dietaryTags,
        boolean available,
        int stock,
        Instant createdAt,
        Instant updatedAt,
        String imageUrl
) {
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, String imageUrl) {
        return from(product, imageUrl, 0);
    }

    public static ProductResponse from(Product product, String imageUrl, int stock) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getCode(),
                InrMoney.toRupees(product.getPricePaise()),
                Set.copyOf(product.getIngredients()),
                product.getCalories(),
                product.getProteinGrams(),
                product.getSpiceLevel(),
                Set.copyOf(product.getDietaryTags()),
                product.isAvailable(),
                stock,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                imageUrl
        );
    }
}
