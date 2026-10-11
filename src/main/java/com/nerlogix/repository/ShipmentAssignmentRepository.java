package com.nerlogix.repository;

import com.nerlogix.entity.ShipmentAssignment;
import com.nerlogix.model.enums.ShipmentAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentAssignmentRepository extends JpaRepository<ShipmentAssignment, Long> {
    List<ShipmentAssignment> findByDriverUserIdAndStatus(Long driverUserId, ShipmentAssignmentStatus status);
    List<ShipmentAssignment> findByShipmentId(Long shipmentId);
    List<ShipmentAssignment> findByStatus(ShipmentAssignmentStatus status);
    Optional<ShipmentAssignment> findFirstByShipmentIdAndStatus(Long shipmentId, ShipmentAssignmentStatus status);
}
