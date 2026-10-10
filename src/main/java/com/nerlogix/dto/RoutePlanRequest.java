package com.nerlogix.dto;

import com.nerlogix.model.enums.ShipmentPriority;
import com.nerlogix.model.enums.VehicleType;
import jakarta.validation.constraints.NotNull;

public record RoutePlanRequest(
    @NotNull(message = "Origin node ID is required")
    Long originNodeId,

    @NotNull(message = "Destination node ID is required")
    Long destinationNodeId,

    VehicleType vehicleType,

    ShipmentPriority priority
) {}
