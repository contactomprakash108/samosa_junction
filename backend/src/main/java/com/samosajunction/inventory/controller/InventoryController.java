package com.samosajunction.inventory.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.inventory.dto.InventoryResponse;
import com.samosajunction.inventory.dto.UpdateInventoryRequest;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.staff.service.StaffAuditService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventoryService;
    private final StaffAuditService staffAuditService;

    public InventoryController(InventoryService inventoryService, StaffAuditService staffAuditService) {
        this.inventoryService = inventoryService;
        this.staffAuditService = staffAuditService;
    }

    @GetMapping("/products/{productId}/inventory")
    public InventoryResponse get(@PathVariable UUID productId) {
        return inventoryService.getByProductId(productId);
    }

    @PutMapping("/inventory/{productId}")
    public InventoryResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateInventoryRequest request
    ) {
        InventoryResponse response = inventoryService.setQuantity(productId, request.quantity());
        staffAuditService.record(
                principal.getId(),
                "INVENTORY_SET",
                "INVENTORY",
                productId,
                String.valueOf(request.quantity())
        );
        return response;
    }
}
