package com.samosajunction.complaint.repository;

import com.samosajunction.complaint.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.samosajunction.complaint.entity.ComplaintStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    Page<Complaint> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Complaint> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Complaint> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    long countByStatusIn(Collection<ComplaintStatus> statuses);
}
