package com.nerlogix.dto;

import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.model.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleRequest(
    @NotBlank String regNo,
    @NotNull VehicleType type,
    @NotNull Double capacityKg,
    @NotNull Double lat,
    @NotNull Double lng,
    VehicleStatus status
) {}
