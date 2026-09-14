package com.samosajunction.support.repository;

import com.samosajunction.support.entity.SupportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {

    Page<SupportMessage> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<SupportMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
