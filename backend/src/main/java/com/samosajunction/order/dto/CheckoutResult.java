package com.samosajunction.order.dto;

public record CheckoutResult(OrderResponse order, boolean replayed) {
}
