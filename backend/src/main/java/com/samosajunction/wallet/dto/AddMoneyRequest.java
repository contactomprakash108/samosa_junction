package com.samosajunction.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddMoneyRequest(
        @NotNull
        @DecimalMin(value = "1.00")
        @DecimalMax(value = "50000.00")
        BigDecimal amount,
        @Size(max = 64) String referenceId
) {
}
