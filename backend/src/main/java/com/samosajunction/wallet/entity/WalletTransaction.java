package com.samosajunction.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@EntityListeners(AuditingEntityListener.class)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletTransactionType type;

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletTransactionStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletTransaction() {
    }

    public WalletTransaction(
            UUID walletId,
            WalletTransactionType type,
            int amountPaise,
            String idempotencyKey,
            String referenceId,
            WalletTransactionStatus status
    ) {
        this.walletId = walletId;
        this.type = type;
        this.amountPaise = amountPaise;
        this.idempotencyKey = idempotencyKey;
        this.referenceId = referenceId;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public int getAmountPaise() {
        return amountPaise;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public WalletTransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
