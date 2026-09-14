package com.samosajunction.complaint.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.common.response.PageResponse;
import com.samosajunction.complaint.dto.ComplaintResponse;
import com.samosajunction.complaint.dto.CreateComplaintRequest;
import com.samosajunction.complaint.dto.CreateComplaintResult;
import com.samosajunction.complaint.dto.UpdateComplaintRequest;
import com.samosajunction.complaint.service.ComplaintService;
import com.samosajunction.staff.service.StaffAuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final StaffAuditService staffAuditService;

    public ComplaintController(ComplaintService complaintService, StaffAuditService staffAuditService) {
        this.complaintService = complaintService;
        this.staffAuditService = staffAuditService;
    }

    @PostMapping
    public ResponseEntity<ComplaintResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateComplaintRequest request
    ) {
        CreateComplaintResult result = complaintService.create(principal.getId(), request, idempotencyKey);
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.complaint());
    }

    @GetMapping
    public PageResponse<ComplaintResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(complaintService.list(principal.getId(), isPrivileged(principal), pageable));
    }

    @GetMapping("/{complaintId}")
    public ComplaintResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID complaintId
    ) {
        return complaintService.get(principal.getId(), complaintId, isPrivileged(principal));
    }

    @PatchMapping("/{complaintId}")
    public ComplaintResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID complaintId,
            @Valid @RequestBody UpdateComplaintRequest request
    ) {
        ComplaintResponse response = complaintService.update(complaintId, request);
        staffAuditService.record(
                principal.getId(),
                "COMPLAINT_UPDATE",
                "COMPLAINT",
                complaintId,
                response.status().name()
        );
        return response;
    }

    private static boolean isPrivileged(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_STAFF") || authority.equals("ROLE_ADMIN"));
    }
}
