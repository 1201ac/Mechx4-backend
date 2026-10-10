package com.nerlogix.dto;

import com.nerlogix.model.enums.IncidentStatus;
import com.nerlogix.model.enums.IncidentType;
import java.time.LocalDateTime;

public record MapIncidentDto(
    Long id,
    IncidentType type,
    Integer severity,
    double lat,
    double lng,
    Long segmentId,
    Long districtId,
    String description,
    IncidentStatus status,
    LocalDateTime createdAt
) {}
