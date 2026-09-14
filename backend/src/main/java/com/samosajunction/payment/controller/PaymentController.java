package com.samosajunction.payment.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.common.response.PageResponse;
import com.samosajunction.order.service.OrderService;
import com.samosajunction.payment.dto.CreatePaymentRequest;
import com.samosajunction.payment.dto.PayResult;
import com.samosajunction.payment.dto.PaymentResponse;
import com.samosajunction.payment.service.PaymentService;
import com.samosajunction.staff.service.StaffAuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final StaffAuditService staffAuditService;

    public PaymentController(
            PaymentService paymentService,
            OrderService orderService,
            StaffAuditService staffAuditService
    ) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.staffAuditService = staffAuditService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PayResult result = paymentService.pay(principal.getId(), request, idempotencyKey);
        if (result.failed()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result.payment());
        }
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.payment());
    }

    @GetMapping
    public PageResponse<PaymentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(paymentService.list(principal.getId(), pageable));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId
    ) {
        return paymentService.get(principal.getId(), paymentId, isPrivileged(principal));
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponse refund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentId
    ) {
        PaymentResponse response = orderService.refundPayment(principal.getId(), paymentId, isPrivileged(principal));
        if (isPrivileged(principal)) {
            staffAuditService.record(principal.getId(), "REFUND", "PAYMENT", paymentId, response.status().name());
        }
        return response;
    }

    private static boolean isPrivileged(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_STAFF") || authority.equals("ROLE_ADMIN"));
    }
}
