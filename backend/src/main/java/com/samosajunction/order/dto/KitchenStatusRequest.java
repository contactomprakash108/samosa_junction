package com.samosajunction.order.dto;

import com.samosajunction.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record KitchenStatusRequest(@NotNull OrderStatus status) {
}
