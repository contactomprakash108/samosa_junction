package com.samosajunction.complaint.dto;

import com.samosajunction.complaint.entity.ComplaintCategory;
import com.samosajunction.complaint.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateComplaintRequest(
        @NotNull UUID orderId,
        @NotNull ComplaintCategory category,
        @NotBlank @Size(min = 10, max = 1000) String description,
        ComplaintPriority priority
) {
}
