package com.samosajunction.staff.service;

import com.samosajunction.staff.entity.StaffAuditEvent;
import com.samosajunction.staff.repository.StaffAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StaffAuditService {

    private final StaffAuditEventRepository staffAuditEventRepository;

    public StaffAuditService(StaffAuditEventRepository staffAuditEventRepository) {
        this.staffAuditEventRepository = staffAuditEventRepository;
    }

    @Transactional
    public void record(UUID actorId, String action, String entityType, UUID entityId, String detail) {
        staffAuditEventRepository.save(new StaffAuditEvent(actorId, action, entityType, entityId, detail));
    }
}
