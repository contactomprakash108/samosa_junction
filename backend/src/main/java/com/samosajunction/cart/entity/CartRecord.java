package com.samosajunction.cart.entity;

import com.samosajunction.cart.store.CartPayload;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class CartRecord {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private CartPayload payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartRecord() {
    }

    public CartRecord(UUID userId, CartPayload payload, Instant updatedAt) {
        this.userId = userId;
        this.payload = payload;
        this.updatedAt = updatedAt;
    }

    public void replace(CartPayload payload, Instant updatedAt) {
        this.payload = payload;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public CartPayload getPayload() {
        return payload;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
