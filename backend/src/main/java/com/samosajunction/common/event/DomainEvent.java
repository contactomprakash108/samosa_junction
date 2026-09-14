package com.samosajunction.common.event;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        DomainEventType type,
        UUID aggregateId,
        UUID userId,
        Instant occurredAt
) {
    public static DomainEvent of(DomainEventType type, UUID aggregateId, UUID userId) {
        return of(type, aggregateId, userId, aggregateId.toString());
    }

    public static DomainEvent of(DomainEventType type, UUID aggregateId, UUID userId, String uniqueness) {
        return new DomainEvent(
                deterministicId(type, uniqueness),
                type,
                aggregateId,
                userId,
                Instant.now()
        );
    }

    public static DomainEvent walletDebited(UUID userId, String idempotencyKey) {
        return new DomainEvent(
                deterministicId(DomainEventType.WALLET_DEBITED, userId + ":" + idempotencyKey),
                DomainEventType.WALLET_DEBITED,
                userId,
                userId,
                Instant.now()
        );
    }

    static UUID deterministicId(DomainEventType type, String uniqueness) {
        return UUID.nameUUIDFromBytes((type.name() + ":" + uniqueness).getBytes(StandardCharsets.UTF_8));
    }
}
