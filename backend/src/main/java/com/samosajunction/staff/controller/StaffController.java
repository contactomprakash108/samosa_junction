package com.samosajunction.staff.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.common.response.PageResponse;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.payment.dto.PaymentResponse;
import com.samosajunction.staff.dto.StaffDashboardResponse;
import com.samosajunction.staff.dto.StaffInventoryItemResponse;
import com.samosajunction.staff.dto.StaffOrderDetailResponse;
import com.samosajunction.staff.service.StaffAuditService;
import com.samosajunction.staff.service.StaffOperationsService;
import com.samosajunction.support.dto.StaffSupportMessageResponse;
import com.samosajunction.support.service.SupportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffController {

    private final StaffOperationsService staffOperationsService;
    private final StaffAuditService staffAuditService;
    private final SupportService supportService;

    public StaffController(
            StaffOperationsService staffOperationsService,
            StaffAuditService staffAuditService,
            SupportService supportService
    ) {
        this.staffOperationsService = staffOperationsService;
        this.staffAuditService = staffAuditService;
        this.supportService = supportService;
    }

    @GetMapping("/dashboard")
    public StaffDashboardResponse dashboard() {
        return staffOperationsService.dashboard();
    }

    @GetMapping("/inventory")
    public List<StaffInventoryItemResponse> inventory() {
        return staffOperationsService.inventory();
    }

    @GetMapping("/orders")
    public PageResponse<OrderResponse> orders(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(staffOperationsService.orders(pageable));
    }

    @GetMapping("/orders/{orderId}")
    public StaffOrderDetailResponse order(@PathVariable UUID orderId) {
        return staffOperationsService.order(orderId);
    }

    @PostMapping("/orders/{orderId}/collect-cod")
    public PaymentResponse collectCod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderId
    ) {
        PaymentResponse payment = staffOperationsService.collectCod(orderId);
        staffAuditService.record(principal.getId(), "COD_COLLECT", "ORDER", orderId, payment.status().name());
        return payment;
    }

    @GetMapping("/support")
    public PageResponse<StaffSupportMessageResponse> support(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(supportService.listAll(pageable));
    }

    @PatchMapping("/support/{messageId}/close")
    public StaffSupportMessageResponse closeSupport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId
    ) {
        StaffSupportMessageResponse closed = supportService.close(messageId);
        staffAuditService.record(principal.getId(), "SUPPORT_CLOSE", "SUPPORT", messageId, closed.status());
        return closed;
    }
}
