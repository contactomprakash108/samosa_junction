package com.samosajunction.wallet.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InsufficientFundsException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.wallet.entity.Wallet;
import com.samosajunction.wallet.entity.WalletTransaction;
import com.samosajunction.wallet.entity.WalletTransactionStatus;
import com.samosajunction.wallet.entity.WalletTransactionType;
import com.samosajunction.wallet.repository.WalletRepository;
import com.samosajunction.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private WalletService walletService;
    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, walletTransactionRepository, domainEventPublisher);
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = new Wallet(userId);
        ReflectionTestUtils.setField(wallet, "id", walletId);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
    }

    @Test
    void addMoneyCreditsBalanceAndWritesLedger() {
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "add-1"))
                .thenReturn(Optional.empty());
        when(walletTransactionRepository.saveAndFlush(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = walletService.addMoney(userId, new BigDecimal("100.00"), "add-1", "sim-1");

        assertThat(response.balance()).isEqualByComparingTo("100.00");
        assertThat(wallet.getBalancePaise()).isEqualTo(10000);
        verify(walletTransactionRepository).saveAndFlush(any(WalletTransaction.class));
        verify(domainEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void duplicateIdempotencyKeyDoesNotDoubleCredit() {
        var existing = new WalletTransaction(
                walletId,
                WalletTransactionType.CREDIT,
                10000,
                "add-1",
                "sim-1",
                WalletTransactionStatus.COMPLETED
        );
        wallet.credit(10000);
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "add-1"))
                .thenReturn(Optional.of(existing));

        var response = walletService.addMoney(userId, new BigDecimal("100.00"), "add-1", "sim-1");

        assertThat(response.balance()).isEqualByComparingTo("100.00");
        verify(walletTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void reusedIdempotencyKeyWithDifferentAmountConflicts() {
        var existing = new WalletTransaction(
                walletId,
                WalletTransactionType.CREDIT,
                10000,
                "add-1",
                "sim-1",
                WalletTransactionStatus.COMPLETED
        );
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "add-1"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> walletService.addMoney(userId, new BigDecimal("50.00"), "add-1", "sim-2"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("different amount");
    }

    @Test
    void debitReplayDoesNotDoubleCharge() {
        wallet.credit(1000);
        var existing = new WalletTransaction(
                walletId,
                WalletTransactionType.DEBIT,
                800,
                "debit-1",
                "order-1",
                WalletTransactionStatus.COMPLETED
        );
        wallet.debit(800);
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "debit-1"))
                .thenReturn(Optional.of(existing));

        var response = walletService.debit(userId, 800, "debit-1", "order-1");

        assertThat(response.balance()).isEqualByComparingTo("2.00");
        assertThat(wallet.getBalancePaise()).isEqualTo(200);
        verify(walletTransactionRepository, never()).saveAndFlush(any());
        verify(domainEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void debitRejectsInsufficientFunds() {
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "debit-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.debit(userId, 100, "debit-1", "order-1"))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(wallet.getBalancePaise()).isZero();
        verify(walletTransactionRepository, never()).saveAndFlush(any());
        verify(domainEventPublisher, never()).publishAfterCommit(any());
    }

    @Test
    void twoSequentialDebitsCannotSpendTheSameRupee() {
        wallet.credit(1000);
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "d1")).thenReturn(Optional.empty());
        when(walletTransactionRepository.findByWalletIdAndIdempotencyKey(walletId, "d2")).thenReturn(Optional.empty());
        when(walletTransactionRepository.saveAndFlush(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        walletService.debit(userId, 800, "d1", "order-a");
        assertThatThrownBy(() -> walletService.debit(userId, 800, "d2", "order-b"))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(wallet.getBalancePaise()).isEqualTo(200);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.WALLET_DEBITED
        ));
    }
}
