package com.nerlogix.dto;

import com.nerlogix.entity.Notification;
import java.time.LocalDateTime;

public record NotificationDto(
    Long id,
    Long alertId,
    String title,
    String message,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
    public static NotificationDto fromEntity(Notification n) {
        return new NotificationDto(
            n.getId(),
            n.getAlert() != null ? n.getAlert().getId() : null,
            n.getTitle(),
            n.getMessage(),
            n.getReadAt(),
            n.getCreatedAt()
        );
    }
}
