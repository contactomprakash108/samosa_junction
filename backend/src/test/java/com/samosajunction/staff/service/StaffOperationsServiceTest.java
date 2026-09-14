package com.samosajunction.staff.service;

import com.samosajunction.complaint.repository.ComplaintRepository;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.payment.repository.PaymentRepository;
import com.samosajunction.payment.service.PaymentService;
import com.samosajunction.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffOperationsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    private StaffOperationsService staffOperationsService;

    @BeforeEach
    void setUp() {
        staffOperationsService = new StaffOperationsService(
                orderRepository,
                paymentRepository,
                paymentService,
                complaintRepository,
                inventoryRepository,
                productRepository
        );
    }

    @Test
    void dashboardUsesLiveCounts() {
        when(orderRepository.countByCreatedAtGreaterThanEqual(any())).thenReturn(47L);
        when(paymentRepository.sumSuccessfulAmountPaiseSince(any(), any())).thenReturn(482000L);
        when(orderRepository.countByStatus(OrderStatus.PREPARING)).thenReturn(6L);
        when(orderRepository.countByStatus(OrderStatus.READY)).thenReturn(3L);
        when(orderRepository.countByStatus(OrderStatus.CREATED)).thenReturn(2L);
        when(complaintRepository.countByStatusIn(any())).thenReturn(1L);
        when(inventoryRepository.countByQuantityLessThanEqual(5)).thenReturn(4L);

        var dashboard = staffOperationsService.dashboard();

        assertThat(dashboard.ordersToday()).isEqualTo(47);
        assertThat(dashboard.revenueToday()).isEqualByComparingTo(new BigDecimal("4820.00"));
        assertThat(dashboard.preparing()).isEqualTo(6);
        assertThat(dashboard.ready()).isEqualTo(3);
        assertThat(dashboard.pending()).isEqualTo(2);
        assertThat(dashboard.openComplaints()).isEqualTo(1);
        assertThat(dashboard.lowStock()).isEqualTo(4);
    }
}
