package com.samosajunction.payment.dto;

import com.samosajunction.payment.entity.Payment;
import com.samosajunction.payment.entity.PaymentMethod;
import com.samosajunction.payment.entity.PaymentStatus;
import com.samosajunction.product.support.InrMoney;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String failureReason,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                InrMoney.toRupees(payment.getAmountPaise()),
                payment.getMethod(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt()
        );
    }
}
