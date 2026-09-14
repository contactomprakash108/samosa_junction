package com.samosajunction.staff.dto;

import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.payment.dto.PaymentResponse;

public record StaffOrderDetailResponse(OrderResponse order, PaymentResponse payment) {
}
