package com.samosajunction.product.dto;

import com.samosajunction.product.entity.SpiceLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record ProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 1000) String description,
        @NotBlank @Size(max = 32) String category,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        @NotEmpty Set<@NotBlank @Size(max = 80) String> ingredients,
        @Min(0) int calories,
        @NotNull @DecimalMin("0.00") BigDecimal protein,
        @NotNull SpiceLevel spiceLevel,
        @NotEmpty Set<@NotBlank @Size(max = 40) String> dietaryTags,
        @NotNull Boolean available
) {
}
