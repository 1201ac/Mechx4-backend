package com.nerlogix.repository;

import com.nerlogix.entity.VehiclePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehiclePositionRepository extends JpaRepository<VehiclePosition, Long> {
    List<VehiclePosition> findTop20ByVehicleIdOrderByRecordedAtDesc(Long vehicleId);
}
