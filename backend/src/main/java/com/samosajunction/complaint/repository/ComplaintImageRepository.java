package com.samosajunction.complaint.repository;

import com.samosajunction.complaint.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, UUID> {

    List<ComplaintImage> findByComplaintIdOrderByUploadedAtAsc(UUID complaintId);

    long countByComplaintId(UUID complaintId);

    Optional<ComplaintImage> findByIdAndComplaintId(UUID id, UUID complaintId);
}
