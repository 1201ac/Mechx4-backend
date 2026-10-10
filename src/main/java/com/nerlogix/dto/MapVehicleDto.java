package com.nerlogix.dto;

import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.model.enums.VehicleType;

public record MapVehicleDto(
    Long id,
    String regNo,
    VehicleType type,
    VehicleStatus status,
    Double lat,
    Double lng,
    Double heading,
    Double speedKmph,
    String driverName,
    Long currentShipmentId,
    String currentShipmentCode
) {}
