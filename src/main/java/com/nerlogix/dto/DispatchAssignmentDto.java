package com.nerlogix.dto;

import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.ShipmentAssignment;
import com.nerlogix.entity.User;
import com.nerlogix.entity.Vehicle;
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
        if (a == null) return null;
        Shipment s = a.getShipment();
        User d = a.getDriverUser();
        Vehicle v = a.getVehicle();

        return new DispatchAssignmentDto(
            a.getId(),
            s != null ? s.getId() : null,
            s != null ? s.getCode() : null,
            (s != null && s.getCargoCategory() != null) ? s.getCargoCategory().name() : null,
            s != null ? s.getCargoDescription() : null,
            s != null ? s.getWeightKg() : null,
            (s != null && s.getOriginNode() != null) ? s.getOriginNode().getName() : null,
            (s != null && s.getDestinationNode() != null) ? s.getDestinationNode().getName() : null,
            d != null ? d.getId() : null,
            d != null ? d.getName() : null,
            v != null ? v.getId() : null,
            v != null ? v.getRegNo() : null,
            a.getStatus() != null ? a.getStatus().name() : null,
            a.getDeclineReason(),
            a.getAssignedAt(),
            a.getRespondedAt()
        );
    }
}
