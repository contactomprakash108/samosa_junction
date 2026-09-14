package com.samosajunction.support.dto;

import com.samosajunction.support.entity.SupportMessage;

import java.time.Instant;
import java.util.UUID;

public record SupportMessageResponse(
        UUID id,
        String subject,
        String body,
        String status,
        Instant createdAt
) {
    public static SupportMessageResponse from(SupportMessage message) {
        return new SupportMessageResponse(
                message.getId(),
                message.getSubject(),
                message.getBody(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
