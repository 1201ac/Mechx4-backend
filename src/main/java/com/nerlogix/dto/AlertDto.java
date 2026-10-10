package com.nerlogix.dto;

import com.nerlogix.entity.Alert;
import com.nerlogix.model.enums.AlertSeverity;
import com.nerlogix.model.enums.AlertStatus;
import com.nerlogix.model.enums.AlertType;
import java.time.LocalDateTime;

public record AlertDto(
    Long id,
    AlertType type,
    AlertSeverity severity,
    String entityType,
    Long entityId,
    String openKey,
    AlertStatus status,
    String title,
    String message,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {
    public static AlertDto fromEntity(Alert a) {
        return new AlertDto(
            a.getId(),
            a.getType(),
            a.getSeverity(),
            a.getEntityType(),
            a.getEntityId(),
            a.getOpenKey(),
            a.getStatus(),
            a.getTitle(),
            a.getMessage(),
            a.getCreatedAt(),
            a.getResolvedAt()
        );
    }
}
