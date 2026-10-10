package com.nerlogix.repository;

import com.nerlogix.entity.Shipment;
import com.nerlogix.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByCode(String code);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);
    List<Shipment> findByVehicleIdAndStatusIn(Long vehicleId, List<ShipmentStatus> statuses);
    Optional<Shipment> findByDriverUserIdAndStatusIn(Long driverUserId, List<ShipmentStatus> statuses);
    List<Shipment> findByDriverUserId(Long driverUserId);
    Page<Shipment> findByDriverUserId(Long driverUserId, Pageable pageable);
}
