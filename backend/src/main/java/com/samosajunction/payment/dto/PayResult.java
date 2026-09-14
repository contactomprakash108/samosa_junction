package com.samosajunction.payment.dto;

import com.samosajunction.payment.entity.PaymentStatus;

public record PayResult(PaymentResponse payment, boolean replayed) {

    public boolean failed() {
        return payment.status() == PaymentStatus.FAILED;
    }
}
