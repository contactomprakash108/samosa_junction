package com.samosajunction.order.dto;

import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.product.support.InrMoney;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                InrMoney.toRupees(item.getUnitPricePaise()),
                InrMoney.toRupees(item.lineTotalPaise())
        );
    }
}
