package com.samosajunction.staff.repository;

import com.samosajunction.staff.entity.StaffAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StaffAuditEventRepository extends JpaRepository<StaffAuditEvent, UUID> {
}
