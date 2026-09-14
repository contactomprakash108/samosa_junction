package com.samosajunction.order.dto;

import com.samosajunction.payment.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull @Valid DeliveryAddressRequest delivery,
        Boolean payNow,
        PaymentMethod paymentMethod
) {
    public CreateOrderRequest(DeliveryAddressRequest delivery) {
        this(delivery, true, null);
    }

    public CreateOrderRequest(DeliveryAddressRequest delivery, Boolean payNow) {
        this(delivery, payNow, null);
    }

    public boolean shouldPayNow() {
        return payNow == null || payNow;
    }

    public PaymentMethod resolvedPaymentMethod() {
        return paymentMethod == null ? PaymentMethod.WALLET : paymentMethod;
    }
}
