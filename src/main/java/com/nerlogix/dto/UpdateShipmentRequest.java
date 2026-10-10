package com.nerlogix.dto;

import com.nerlogix.model.enums.CargoCategory;
import com.nerlogix.model.enums.ShipmentPriority;
import java.time.LocalDateTime;

public record UpdateShipmentRequest(
    CargoCategory cargoCategory,
    String cargoDescription,
    Double weightKg,
    ShipmentPriority priority,
    Long originNodeId,
    Long destinationNodeId,
    Long vehicleId,
    Long driverUserId,
    LocalDateTime plannedEta
) {}
