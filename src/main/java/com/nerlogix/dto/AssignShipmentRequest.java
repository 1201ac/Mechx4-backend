package com.nerlogix.dto;

import jakarta.validation.constraints.NotNull;

public record AssignShipmentRequest(
    @NotNull Long vehicleId,
    @NotNull Long driverUserId,
    Long shipmentId
) {
    public AssignShipmentRequest(Long vehicleId, Long driverUserId) {
        this(vehicleId, driverUserId, null);
    }
}
