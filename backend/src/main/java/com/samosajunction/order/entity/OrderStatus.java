package com.samosajunction.order.entity;


import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    CONFIRMED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(CREATED, CONFIRMED, PREPARING);

    public boolean canCancel() {
        return CANCELLABLE.contains(this);
    }

    public boolean isKitchenAdvance(OrderStatus next) {
        return next != null && next == kitchenSuccessor();
    }

    public OrderStatus kitchenSuccessor() {
        return switch (this) {
            case CONFIRMED -> PREPARING;
            case PREPARING -> READY;
            case READY -> OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> DELIVERED;
            default -> null;
        };
    }
}
