package com.samosajunction.complaint.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.complaint.dto.CreateComplaintRequest;
import com.samosajunction.complaint.dto.UpdateComplaintRequest;
import com.samosajunction.complaint.entity.Complaint;
import com.samosajunction.complaint.entity.ComplaintCategory;
import com.samosajunction.complaint.entity.ComplaintPriority;
import com.samosajunction.complaint.entity.ComplaintStatus;
import com.samosajunction.complaint.repository.ComplaintRepository;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private ComplaintImageService complaintImageService;

    private ComplaintService complaintService;
    private UUID userId;
    private UUID orderId;
    private UUID complaintId;
    private CreateComplaintRequest request;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService(
                complaintRepository,
                orderRepository,
                domainEventPublisher,
                complaintImageService
        );
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        complaintId = UUID.randomUUID();
        request = new CreateComplaintRequest(
                orderId,
                ComplaintCategory.DAMAGED,
                "My samosas arrived damaged.",
                ComplaintPriority.HIGH
        );
    }

    @Test
    void createOpensComplaintForOwnPaidOrder() {
        when(complaintRepository.findByUserIdAndIdempotencyKey(userId, "c-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(confirmedOrder(userId)));
        when(complaintRepository.saveAndFlush(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint complaint = invocation.getArgument(0);
            ReflectionTestUtils.setField(complaint, "id", complaintId);
            return complaint;
        });

        var result = complaintService.create(userId, request, "c-1");

        assertThat(result.replayed()).isFalse();
        assertThat(result.complaint().status()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(result.complaint().category()).isEqualTo(ComplaintCategory.DAMAGED);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.COMPLAINT_CREATED
        ));
    }

    @Test
    void createHidesAnotherUsersOrder() {
        when(complaintRepository.findByUserIdAndIdempotencyKey(userId, "c-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(confirmedOrder(UUID.randomUUID())));

        assertThatThrownBy(() -> complaintService.create(userId, request, "c-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");
        verify(complaintRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnpaidOrder() {
        Order unpaid = confirmedOrder(userId);
        ReflectionTestUtils.setField(unpaid, "status", OrderStatus.CREATED);
        when(complaintRepository.findByUserIdAndIdempotencyKey(userId, "c-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(unpaid));

        assertThatThrownBy(() -> complaintService.create(userId, request, "c-1"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("unpaid");
    }

    @Test
    void createReplaysIdempotencyKey() {
        Complaint existing = openComplaint();
        when(complaintRepository.findByUserIdAndIdempotencyKey(userId, "c-1")).thenReturn(Optional.of(existing));

        var result = complaintService.create(userId, request, "c-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.complaint().id()).isEqualTo(complaintId);
        verify(domainEventPublisher, never()).publishAfterCommit(any(DomainEvent.class));
    }

    @Test
    void getHidesAnotherUsersComplaint() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(openComplaint()));

        assertThatThrownBy(() -> complaintService.get(UUID.randomUUID(), complaintId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Complaint not found");
    }

    @Test
    void staffCanReadAnyComplaint() {
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(openComplaint()));
        when(complaintImageService.listForComplaint(complaintId)).thenReturn(java.util.List.of());

        var response = complaintService.get(UUID.randomUUID(), complaintId, true);

        assertThat(response.id()).isEqualTo(complaintId);
    }

    @Test
    void staffAdvancesOpenToInProgress() {
        Complaint complaint = openComplaint();
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        var response = complaintService.update(
                complaintId,
                new UpdateComplaintRequest(ComplaintStatus.IN_PROGRESS, null)
        );

        assertThat(response.status()).isEqualTo(ComplaintStatus.IN_PROGRESS);
        verify(domainEventPublisher).publishAfterCommit(org.mockito.ArgumentMatchers.argThat(
                event -> event.type() == DomainEventType.COMPLAINT_UPDATED
        ));
    }

    @Test
    void staffCannotReopenResolved() {
        Complaint complaint = openComplaint();
        complaint.transitionTo(ComplaintStatus.RESOLVED);
        when(complaintRepository.findById(complaintId)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.update(
                complaintId,
                new UpdateComplaintRequest(ComplaintStatus.OPEN, null)
        )).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("cannot move");
        verify(domainEventPublisher, never()).publishAfterCommit(any());
    }

    private Complaint openComplaint() {
        Complaint complaint = new Complaint(
                userId,
                orderId,
                ComplaintCategory.DAMAGED,
                "My samosas arrived damaged.",
                ComplaintPriority.HIGH,
                "c-1"
        );
        ReflectionTestUtils.setField(complaint, "id", complaintId);
        return complaint;
    }

    private Order confirmedOrder(UUID ownerId) {
        Order order = new Order(ownerId, "Ada", "1 Road", "Pune", "MH", "411001", "order-1");
        order.confirm();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }
}
