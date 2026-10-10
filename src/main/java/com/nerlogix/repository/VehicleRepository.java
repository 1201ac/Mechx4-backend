package com.nerlogix.repository;

import com.nerlogix.entity.Vehicle;
import com.nerlogix.model.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByRegNo(String regNo);
    List<Vehicle> findByStatus(VehicleStatus status);
}
