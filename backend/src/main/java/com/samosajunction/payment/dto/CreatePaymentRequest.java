package com.samosajunction.payment.dto;

import com.samosajunction.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        PaymentMethod method,
        Boolean simulateFailure
) {
    public PaymentMethod resolvedMethod() {
        return method == null ? PaymentMethod.WALLET : method;
    }

    public boolean shouldSimulateFailure() {
        return Boolean.TRUE.equals(simulateFailure);
    }
}
