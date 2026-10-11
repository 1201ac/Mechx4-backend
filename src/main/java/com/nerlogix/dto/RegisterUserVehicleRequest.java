package com.nerlogix.dto;

import com.nerlogix.model.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterUserVehicleRequest(
    @NotBlank String regNo,
    @NotNull VehicleType type,
    @NotNull Double capacityKg,
    Double lat,
    Double lng
) {}
