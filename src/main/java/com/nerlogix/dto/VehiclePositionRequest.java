package com.nerlogix.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record VehiclePositionRequest(
    @NotNull Double lat,
    @NotNull Double lng,
    Double heading,
    Double speedKmph,
    LocalDateTime recordedAt
) {}
