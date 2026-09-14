package com.samosajunction.payment.repository;

import com.samosajunction.payment.entity.Payment;
import com.samosajunction.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            select coalesce(sum(p.amountPaise), 0)
            from Payment p
            where p.status = :status
              and p.createdAt >= :start
            """)
    Long sumSuccessfulAmountPaiseSince(@Param("status") PaymentStatus status, @Param("start") Instant start);
}
