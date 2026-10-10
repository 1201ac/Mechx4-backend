package com.nerlogix.dto;

import com.nerlogix.entity.Vehicle;
import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.model.enums.VehicleType;
import java.time.LocalDateTime;

public record VehicleDto(
    Long id,
    String regNo,
    VehicleType type,
    Double capacityKg,
    VehicleStatus status,
    Double lat,
    Double lng,
    Double heading,
    Double speedKmph,
    LocalDateTime lastSeen
) {
    public static VehicleDto fromEntity(Vehicle v) {
        return new VehicleDto(
            v.getId(),
            v.getRegNo(),
            v.getType(),
            v.getCapacityKg(),
            v.getStatus(),
            v.getLat(),
            v.getLng(),
            v.getHeading(),
            v.getSpeedKmph(),
            v.getLastSeen()
        );
    }
}
