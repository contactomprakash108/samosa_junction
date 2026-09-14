package com.samosajunction.order.controller;

import com.samosajunction.common.response.PageResponse;
import com.samosajunction.order.dto.KitchenStatusRequest;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.order.service.KitchenService;
import com.samosajunction.staff.service.StaffAuditService;
import com.samosajunction.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/kitchen/orders")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class KitchenController {

    private final KitchenService kitchenService;
    private final StaffAuditService staffAuditService;

    public KitchenController(KitchenService kitchenService, StaffAuditService staffAuditService) {
        this.kitchenService = kitchenService;
        this.staffAuditService = staffAuditService;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(kitchenService.list(pageable));
    }

    @PatchMapping("/{orderId}")
    public OrderResponse advance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody KitchenStatusRequest request
    ) {
        OrderResponse response = kitchenService.advance(orderId, request.status());
        staffAuditService.record(principal.getId(), "ORDER_STATUS", "ORDER", orderId, request.status().name());
        return response;
    }
}
