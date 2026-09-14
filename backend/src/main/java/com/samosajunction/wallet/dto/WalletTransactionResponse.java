package com.samosajunction.wallet.dto;

import com.samosajunction.product.support.InrMoney;
import com.samosajunction.wallet.entity.WalletTransaction;
import com.samosajunction.wallet.entity.WalletTransactionStatus;
import com.samosajunction.wallet.entity.WalletTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        WalletTransactionType type,
        BigDecimal amount,
        String referenceId,
        WalletTransactionStatus status,
        Instant createdAt
) {
    public static WalletTransactionResponse from(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                InrMoney.toRupees(transaction.getAmountPaise()),
                transaction.getReferenceId(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
