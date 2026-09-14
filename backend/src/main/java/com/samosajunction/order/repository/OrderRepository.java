package com.samosajunction.order.repository;

import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.userId = :userId and o.idempotencyKey = :idempotencyKey")
    Optional<Order> findWithItemsByUserIdAndIdempotencyKey(
            @Param("userId") UUID userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.userId = :userId and o.status in :statuses")
    List<Order> findWithItemsByUserIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<OrderStatus> statuses
    );

    @Query("select o from Order o where o.status in :statuses order by o.createdAt asc")
    Page<Order> findKitchenQueue(@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);

    @Query("select o from Order o order by o.createdAt desc")
    Page<Order> findRecent(Pageable pageable);

    long countByCreatedAtGreaterThanEqual(Instant start);

    long countByStatus(OrderStatus status);
}
