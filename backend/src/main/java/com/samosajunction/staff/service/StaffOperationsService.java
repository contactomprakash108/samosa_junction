package com.samosajunction.staff.service;

import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.complaint.entity.ComplaintStatus;
import com.samosajunction.complaint.repository.ComplaintRepository;
import com.samosajunction.inventory.entity.Inventory;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.payment.dto.PaymentResponse;
import com.samosajunction.payment.entity.PaymentStatus;
import com.samosajunction.payment.repository.PaymentRepository;
import com.samosajunction.payment.service.PaymentService;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.staff.dto.StaffDashboardResponse;
import com.samosajunction.staff.dto.StaffInventoryItemResponse;
import com.samosajunction.staff.dto.StaffOrderDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StaffOperationsService {

    private static final ZoneId KITCHEN_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int LOW_STOCK_AT_OR_BELOW = 5;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ComplaintRepository complaintRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public StaffOperationsService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService,
            ComplaintRepository complaintRepository,
            InventoryRepository inventoryRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.complaintRepository = complaintRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public StaffDashboardResponse dashboard() {
        Instant start = LocalDate.now(KITCHEN_ZONE).atStartOfDay(KITCHEN_ZONE).toInstant();
        Instant asOf = Instant.now();
        return new StaffDashboardResponse(
                asOf,
                orderRepository.countByCreatedAtGreaterThanEqual(start),
                BigDecimal.valueOf(Optional.ofNullable(
                                paymentRepository.sumSuccessfulAmountPaiseSince(PaymentStatus.SUCCESS, start)
                        ).orElse(0L))
                        .movePointLeft(2)
                        .setScale(2, RoundingMode.HALF_UP),
                orderRepository.countByStatus(OrderStatus.PREPARING),
                orderRepository.countByStatus(OrderStatus.READY),
                orderRepository.countByStatus(OrderStatus.CREATED),
                complaintRepository.countByStatusIn(EnumSet.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS)),
                inventoryRepository.countByQuantityLessThanEqual(LOW_STOCK_AT_OR_BELOW)
        );
    }

    @Transactional(readOnly = true)
    public List<StaffInventoryItemResponse> inventory() {
        Map<UUID, Inventory> byProduct = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
        return productRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(product -> toInventoryRow(product, byProduct.get(product.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> orders(Pageable pageable) {
        return orderRepository.findRecent(pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public StaffOrderDetailResponse order(UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        PaymentResponse payment = paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElse(null);
        return new StaffOrderDetailResponse(OrderResponse.from(order), payment);
    }

    @Transactional
    public PaymentResponse collectCod(UUID orderId) {
        return paymentService.collectCod(orderId);
    }

    private static StaffInventoryItemResponse toInventoryRow(Product product, Inventory inventory) {
        int quantity = inventory == null ? 0 : inventory.getQuantity();
        boolean soldOut = quantity <= 0;
        boolean lowStock = quantity > 0 && quantity <= LOW_STOCK_AT_OR_BELOW;
        return new StaffInventoryItemResponse(
                product.getId(),
                product.getName(),
                quantity,
                product.isAvailable(),
                lowStock,
                soldOut
        );
    }
}
