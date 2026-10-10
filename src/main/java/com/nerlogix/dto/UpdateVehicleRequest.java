package com.nerlogix.dto;

import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.model.enums.VehicleType;

public record UpdateVehicleRequest(
    String regNo,
    VehicleType type,
    Double capacityKg,
    VehicleStatus status,
    Double lat,
    Double lng,
    Double heading,
    Double speedKmph
) {}
