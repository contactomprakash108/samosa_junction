package com.samosajunction.support.dto;

import com.samosajunction.support.entity.SupportMessage;

import java.time.Instant;
import java.util.UUID;

public record StaffSupportMessageResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        String subject,
        String body,
        String status,
        Instant createdAt
) {
    public static StaffSupportMessageResponse from(SupportMessage message, String email, String fullName) {
        return new StaffSupportMessageResponse(
                message.getId(),
                message.getUserId(),
                email,
                fullName,
                message.getSubject(),
                message.getBody(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
