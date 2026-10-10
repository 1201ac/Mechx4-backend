package com.nerlogix.dto;

import com.nerlogix.model.enums.IncidentType;
import jakarta.validation.constraints.NotNull;

public record CreateIncidentRequest(
    @NotNull IncidentType type,
    @NotNull Integer severity,
    @NotNull Double lat,
    @NotNull Double lng,
    Long segmentId,
    Long districtId,
    String description
) {}
