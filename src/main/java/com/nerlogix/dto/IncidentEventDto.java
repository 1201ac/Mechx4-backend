package com.nerlogix.dto;

import com.nerlogix.entity.IncidentEvent;
import java.time.LocalDateTime;

public record IncidentEventDto(
    Long id,
    Long incidentId,
    String action,
    UserDto byUser,
    String note,
    LocalDateTime at
) {
    public static IncidentEventDto fromEntity(IncidentEvent e) {
        return new IncidentEventDto(
            e.getId(),
            e.getIncident() != null ? e.getIncident().getId() : null,
            e.getAction(),
            e.getByUser() != null ? UserDto.fromEntity(e.getByUser()) : null,
            e.getNote(),
            e.getAt()
        );
    }
}
