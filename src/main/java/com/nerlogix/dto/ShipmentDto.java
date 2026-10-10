package com.nerlogix.dto;

import com.nerlogix.entity.Shipment;
import com.nerlogix.model.enums.CargoCategory;
import com.nerlogix.model.enums.ShipmentPriority;
import com.nerlogix.model.enums.ShipmentStatus;
import java.time.LocalDateTime;

public record ShipmentDto(
    Long id,
    String code,
    CargoCategory cargoCategory,
    String cargoDescription,
    Double weightKg,
    ShipmentPriority priority,
    RoadNodeDto originNode,
    RoadNodeDto destinationNode,
    VehicleDto vehicle,
    UserDto driverUser,
    ShipmentStatus status,
    LocalDateTime plannedEta,
    LocalDateTime currentEta,
    Double progressPct,
    Integer delayMin,
    LocalDateTime startedAt,
    LocalDateTime deliveredAt,
    LocalDateTime createdAt
) {
    public static ShipmentDto fromEntity(Shipment s) {
        return new ShipmentDto(
            s.getId(),
            s.getCode(),
            s.getCargoCategory(),
            s.getCargoDescription(),
            s.getWeightKg(),
            s.getPriority(),
            s.getOriginNode() != null ? RoadNodeDto.fromEntity(s.getOriginNode()) : null,
            s.getDestinationNode() != null ? RoadNodeDto.fromEntity(s.getDestinationNode()) : null,
            s.getVehicle() != null ? VehicleDto.fromEntity(s.getVehicle()) : null,
            s.getDriverUser() != null ? UserDto.fromEntity(s.getDriverUser()) : null,
            s.getStatus(),
            s.getPlannedEta(),
            s.getCurrentEta(),
            s.getProgressPct(),
            s.getDelayMin(),
            s.getStartedAt(),
            s.getDeliveredAt(),
            s.getCreatedAt()
        );
    }
}
