package com.samosajunction.complaint.service;

import com.samosajunction.common.event.DomainEvent;
import com.samosajunction.common.event.DomainEventPublisher;
import com.samosajunction.common.event.DomainEventType;
import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.complaint.dto.ComplaintResponse;
import com.samosajunction.complaint.dto.CreateComplaintRequest;
import com.samosajunction.complaint.dto.CreateComplaintResult;
import com.samosajunction.complaint.dto.UpdateComplaintRequest;
import com.samosajunction.complaint.entity.Complaint;
import com.samosajunction.complaint.entity.ComplaintPriority;
import com.samosajunction.complaint.repository.ComplaintRepository;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final ComplaintImageService complaintImageService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            OrderRepository orderRepository,
            DomainEventPublisher domainEventPublisher,
            @Lazy ComplaintImageService complaintImageService
    ) {
        this.complaintRepository = complaintRepository;
        this.orderRepository = orderRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.complaintImageService = complaintImageService;
    }

    @Transactional
    public CreateComplaintResult create(UUID userId, CreateComplaintRequest request, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        var existing = complaintRepository.findByUserIdAndIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            return new CreateComplaintResult(ComplaintResponse.from(existing.get()), true);
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found");
        }
        if (order.getStatus() == OrderStatus.CREATED) {
            throw new InvalidRequestException("Cannot file a complaint against an unpaid order");
        }

        Complaint complaint = new Complaint(
                userId,
                order.getId(),
                request.category(),
                request.description().trim(),
                request.priority() == null ? ComplaintPriority.MEDIUM : request.priority(),
                key
        );
        try {
            complaintRepository.saveAndFlush(complaint);
        } catch (DataIntegrityViolationException ex) {
            Complaint raced = complaintRepository.findByUserIdAndIdempotencyKey(userId, key)
                    .orElseThrow(() -> ex);
            return new CreateComplaintResult(ComplaintResponse.from(raced), true);
        }

        domainEventPublisher.publishAfterCommit(
                DomainEvent.of(DomainEventType.COMPLAINT_CREATED, complaint.getId(), userId)
        );
        return new CreateComplaintResult(ComplaintResponse.from(complaint), false);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintResponse> list(UUID actorId, boolean privileged, Pageable pageable) {
        Page<Complaint> page = privileged
                ? complaintRepository.findAllByOrderByCreatedAtDesc(pageable)
                : complaintRepository.findByUserIdOrderByCreatedAtDesc(actorId, pageable);
        return page.map(ComplaintResponse::from);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse get(UUID actorId, UUID complaintId, boolean privileged) {
        Complaint complaint = requireVisible(actorId, complaintId, privileged);
        return ComplaintResponse.from(complaint, complaintImageService.listForComplaint(complaint.getId()));
    }

    @Transactional
    public ComplaintResponse update(UUID complaintId, UpdateComplaintRequest request) {
        if (request.status() == null && request.priority() == null) {
            throw new InvalidRequestException("Provide a status and/or priority");
        }
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        boolean statusChanged = false;
        if (request.status() != null && request.status() != complaint.getStatus()) {
            if (!complaint.getStatus().canTransitionTo(request.status())) {
                throw new InvalidRequestException(
                        "Complaint cannot move from " + complaint.getStatus() + " to " + request.status()
                );
            }
            complaint.transitionTo(request.status());
            statusChanged = true;
        }
        if (request.priority() != null) {
            complaint.setPriority(request.priority());
        }
        if (statusChanged) {
            domainEventPublisher.publishAfterCommit(DomainEvent.of(
                    DomainEventType.COMPLAINT_UPDATED,
                    complaint.getId(),
                    complaint.getUserId(),
                    complaint.getId() + ":" + request.status().name()
            ));
        }
        return ComplaintResponse.from(complaint);
    }

    public Complaint requireVisible(UUID actorId, UUID complaintId, boolean privileged) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        if (!privileged && !complaint.getUserId().equals(actorId)) {
            throw new ResourceNotFoundException("Complaint not found");
        }
        return complaint;
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
