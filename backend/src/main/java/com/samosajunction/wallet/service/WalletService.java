package com.samosajunction.wallet.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.exception.InsufficientFundsException;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.product.support.InrMoney;
import com.samosajunction.wallet.dto.WalletResponse;
import com.samosajunction.wallet.dto.WalletTransactionResponse;
import com.samosajunction.wallet.entity.Wallet;
import com.samosajunction.wallet.entity.WalletTransaction;
import com.samosajunction.wallet.entity.WalletTransactionStatus;
import com.samosajunction.wallet.entity.WalletTransactionType;
import com.samosajunction.wallet.repository.WalletRepository;
import com.samosajunction.wallet.repository.WalletTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final DomainEventPublisher domainEventPublisher;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public void createForNewUser(UUID userId) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            return;
        }
        walletRepository.save(new Wallet(userId));
    }

    @Transactional
    public WalletResponse getWallet(UUID userId) {
        return WalletResponse.from(getOrCreate(userId));
    }

    @Transactional
    public Page<WalletTransactionResponse> listTransactions(UUID userId, Pageable pageable) {
        Wallet wallet = getOrCreate(userId);
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(WalletTransactionResponse::from);
    }

    /**
     * Simulated top-up. Not a payment-gateway charge.
     */
    @Transactional
    public WalletResponse addMoney(UUID userId, BigDecimal amountRupees, String idempotencyKey, String referenceId) {
        return apply(userId, WalletTransactionType.CREDIT, InrMoney.toPaise(amountRupees), idempotencyKey, referenceId);
    }

    /**
     * Checkout / AI-order debit. Same transaction as the caller (order later).
     */
    @Transactional
    public WalletResponse debit(UUID userId, int amountPaise, String idempotencyKey, String referenceId) {
        return apply(userId, WalletTransactionType.DEBIT, amountPaise, idempotencyKey, referenceId);
    }

    @Transactional
    public WalletResponse refund(UUID userId, int amountPaise, String idempotencyKey, String referenceId) {
        return apply(userId, WalletTransactionType.REFUND, amountPaise, idempotencyKey, referenceId);
    }

    private WalletResponse apply(
            UUID userId,
            WalletTransactionType type,
            int amountPaise,
            String idempotencyKey,
            String referenceId
    ) {
        if (amountPaise < 1) {
            throw new InvalidRequestException("Amount must be at least 0.01");
        }
        String key = requireIdempotencyKey(idempotencyKey);
        Wallet wallet = getOrCreate(userId);

        var existing = walletTransactionRepository.findByWalletIdAndIdempotencyKey(wallet.getId(), key);
        if (existing.isPresent()) {
            return replayOrConflict(wallet, existing.get(), type, amountPaise);
        }

        if (type == WalletTransactionType.DEBIT && wallet.getBalancePaise() < amountPaise) {
            throw new InsufficientFundsException(
                    "Wallet balance is insufficient. Available: %s %s".formatted(
                            InrMoney.toRupees(wallet.getBalancePaise()),
                            wallet.getCurrency()
                    )
            );
        }

        var ledger = new WalletTransaction(
                wallet.getId(),
                type,
                amountPaise,
                key,
                referenceId,
                WalletTransactionStatus.COMPLETED
        );
        try {
            walletTransactionRepository.saveAndFlush(ledger);
        } catch (DataIntegrityViolationException ex) {
            WalletTransaction raced = walletTransactionRepository
                    .findByWalletIdAndIdempotencyKey(wallet.getId(), key)
                    .orElseThrow(() -> ex);
            return replayOrConflict(wallet, raced, type, amountPaise);
        }

        applyBalance(wallet, type, amountPaise);
        if (type == WalletTransactionType.DEBIT) {
            domainEventPublisher.publishAfterCommit(DomainEvent.walletDebited(userId, key));
        }
        return WalletResponse.from(wallet);
    }

    private WalletResponse replayOrConflict(
            Wallet wallet,
            WalletTransaction existing,
            WalletTransactionType type,
            int amountPaise
    ) {
        if (existing.getType() != type || existing.getAmountPaise() != amountPaise) {
            throw new ResourceConflictException(
                    "Idempotency key was already used with a different amount or type"
            );
        }
        return WalletResponse.from(wallet);
    }

    private static void applyBalance(Wallet wallet, WalletTransactionType type, int amountPaise) {
        switch (type) {
            case CREDIT, REFUND -> wallet.credit(amountPaise);
            case DEBIT -> wallet.debit(amountPaise);
        }
    }

    private Wallet getOrCreate(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
    }

    private static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidRequestException("Idempotency-Key header is required");
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 128) {
            throw new InvalidRequestException("Idempotency-Key is too long");
        }
        return trimmed;
    }
}
