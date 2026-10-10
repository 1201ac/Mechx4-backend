package com.nerlogix.dto;

import com.nerlogix.entity.VehiclePosition;
import java.time.LocalDateTime;

public record VehiclePositionDto(
    Long id,
    Long vehicleId,
    Double lat,
    Double lng,
    Double heading,
    Double speedKmph,
    LocalDateTime recordedAt
) {
    public static VehiclePositionDto fromEntity(VehiclePosition vp) {
        return new VehiclePositionDto(
            vp.getId(),
            vp.getVehicle() != null ? vp.getVehicle().getId() : null,
            vp.getLat(),
            vp.getLng(),
            vp.getHeading() != null ? vp.getHeading() : 0.0,
            vp.getSpeedKmph(),
            vp.getRecordedAt()
        );
    }
}
