package com.nerlogix.dto;

import com.nerlogix.entity.Incident;
import com.nerlogix.model.enums.IncidentSource;
import com.nerlogix.model.enums.IncidentStatus;
import com.nerlogix.model.enums.IncidentType;
import java.time.LocalDateTime;

public record IncidentDto(
    Long id,
    IncidentType type,
    Integer severity,
    Double lat,
    Double lng,
    Long segmentId,
    String segmentName,
    Long districtId,
    String districtName,
    String description,
    IncidentStatus status,
    IncidentSource source,
    Long reportId,
    UserDto createdBy,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {
    public static IncidentDto fromEntity(Incident inc) {
        return new IncidentDto(
            inc.getId(),
            inc.getType(),
            inc.getSeverity(),
            inc.getLat(),
            inc.getLng(),
            inc.getSegment() != null ? inc.getSegment().getId() : null,
            inc.getSegment() != null ? inc.getSegment().getName() : null,
            inc.getDistrict() != null ? inc.getDistrict().getId() : null,
            inc.getDistrict() != null ? inc.getDistrict().getName() : null,
            inc.getDescription(),
            inc.getStatus(),
            inc.getSource(),
            inc.getReportId(),
            inc.getCreatedBy() != null ? UserDto.fromEntity(inc.getCreatedBy()) : null,
            inc.getCreatedAt(),
            inc.getResolvedAt()
        );
    }
}
