package com.nerlogix.dto;

import com.nerlogix.model.enums.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record FieldReportPayload(
    @NotBlank String clientId,
    @NotNull Double lat,
    @NotNull Double lng,
    Double accuracy,
    LocalDateTime capturedAt,
    @NotNull IncidentType type,
    @NotNull Integer severity,
    String note
) {}
