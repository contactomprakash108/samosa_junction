package com.samosajunction.order.dto;

import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.product.support.InrMoney;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        String recipientName,
        String addressLine1,
        String city,
        String state,
        String pincode,
        BigDecimal total,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getRecipientName(),
                order.getAddressLine1(),
                order.getCity(),
                order.getState(),
                order.getPincode(),
                InrMoney.toRupees(order.getTotalPaise()),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
