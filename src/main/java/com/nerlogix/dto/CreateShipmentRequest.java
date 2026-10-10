package com.nerlogix.dto;

import com.nerlogix.model.enums.CargoCategory;
import com.nerlogix.model.enums.ShipmentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateShipmentRequest(
    String code,
    @NotNull CargoCategory cargoCategory,
    String cargoDescription,
    @NotNull Double weightKg,
    ShipmentPriority priority,
    @NotNull Long originNodeId,
    @NotNull Long destinationNodeId,
    Long vehicleId,
    Long driverUserId,
    LocalDateTime plannedEta
) {}
