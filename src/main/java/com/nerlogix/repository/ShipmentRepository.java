package com.nerlogix.repository;

import com.nerlogix.entity.Shipment;
import com.nerlogix.model.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>, JpaSpecificationExecutor<Shipment> {
    Optional<Shipment> findByCode(String code);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);
    List<Shipment> findByVehicleIdAndStatusIn(Long vehicleId, List<ShipmentStatus> statuses);
    Optional<Shipment> findFirstByDriverUserIdAndStatusIn(Long driverUserId, List<ShipmentStatus> statuses);
    Optional<Shipment> findByDriverUserIdAndStatusIn(Long driverUserId, List<ShipmentStatus> statuses);
    List<Shipment> findByDriverUserId(Long driverUserId);
    Page<Shipment> findByDriverUserId(Long driverUserId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"originNode", "destinationNode", "vehicle", "driverUser"})
    Page<Shipment> findAll(Specification<Shipment> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"originNode", "destinationNode", "vehicle", "driverUser"})
    Optional<Shipment> findById(Long id);
}
