package com.nerlogix.service;

import com.nerlogix.entity.AuditLog;
import com.nerlogix.entity.User;
import com.nerlogix.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void recordAudit(User user, String action, String entityType, Long entityId, String details, String ipAddress) {
        AuditLog audit = new AuditLog(user, action, entityType, entityId, details, ipAddress);
        auditLogRepository.save(audit);
    }
}
