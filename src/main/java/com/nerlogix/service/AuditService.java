package com.nerlogix.service;

import com.nerlogix.entity.AuditLog;
import com.nerlogix.entity.User;
import com.nerlogix.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(User user, String action, String entityType, Long entityId, String details) {
        log(user, action, entityType, entityId, details, null);
    }

    @Transactional
    public void log(User user, String action, String entityType, Long entityId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog(user, action, entityType, entityId, details, ipAddress);
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("userId", log.getUser() != null ? log.getUser().getId() : null);
            map.put("userEmail", log.getUser() != null ? log.getUser().getEmail() : "System");
            map.put("userName", log.getUser() != null ? log.getUser().getName() : "System");
            map.put("action", log.getAction());
            map.put("entityType", log.getEntityType());
            map.put("entityId", log.getEntityId());
            map.put("details", log.getDetails());
            map.put("ipAddress", log.getIpAddress());
            map.put("createdAt", log.getCreatedAt());
            return map;
        });
    }
}
