package com.nerlogix.dto;

import com.nerlogix.entity.ShipmentAssignment;
import java.time.LocalDateTime;

public record DispatchAssignmentDto(
    Long id,
    Long shipmentId,
    String shipmentCode,
    String cargoCategory,
    String cargoDescription,
    Double weightKg,
    String originName,
    String destinationName,
    Long driverUserId,
    String driverName,
    Long vehicleId,
    String vehicleRegNo,
    String status,
    String declineReason,
    LocalDateTime assignedAt,
    LocalDateTime respondedAt
) {
    public static DispatchAssignmentDto fromEntity(ShipmentAssignment a) {
        return new DispatchAssignmentDto(
            a.getId(),
            a.getShipment().getId(),
            a.getShipment().getCode(),
            a.getShipment().getCargoCategory() != null ? a.getShipment().getCargoCategory().name() : null,
            a.getShipment().getCargoDescription(),
            a.getShipment().getWeightKg(),
            a.getShipment().getOriginNode() != null ? a.getShipment().getOriginNode().getName() : null,
            a.getShipment().getDestinationNode() != null ? a.getShipment().getDestinationNode().getName() : null,
            a.getDriverUser().getId(),
            a.getDriverUser().getName(),
            a.getVehicle().getId(),
            a.getVehicle().getRegNo(),
            a.getStatus().name(),
            a.getDeclineReason(),
            a.getAssignedAt(),
            a.getRespondedAt()
        );
    }
}
