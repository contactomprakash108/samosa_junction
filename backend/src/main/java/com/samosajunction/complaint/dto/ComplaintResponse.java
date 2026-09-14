package com.samosajunction.complaint.dto;

import com.samosajunction.complaint.entity.Complaint;
import com.samosajunction.complaint.entity.ComplaintCategory;
import com.samosajunction.complaint.entity.ComplaintPriority;
import com.samosajunction.complaint.entity.ComplaintStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ComplaintResponse(
        UUID id,
        UUID userId,
        UUID orderId,
        ComplaintCategory category,
        String description,
        ComplaintStatus status,
        ComplaintPriority priority,
        Instant createdAt,
        Instant updatedAt,
        List<ComplaintImageResponse> images
) {
    public static ComplaintResponse from(Complaint complaint) {
        return from(complaint, List.of());
    }

    public static ComplaintResponse from(Complaint complaint, List<ComplaintImageResponse> images) {
        return new ComplaintResponse(
                complaint.getId(),
                complaint.getUserId(),
                complaint.getOrderId(),
                complaint.getCategory(),
                complaint.getDescription(),
                complaint.getStatus(),
                complaint.getPriority(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                images == null ? List.of() : List.copyOf(images)
        );
    }
}
