package com.nerlogix.repository;

import com.nerlogix.entity.RoutePlan;
import com.nerlogix.model.enums.RoutePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    List<RoutePlan> findByShipmentId(Long shipmentId);
    List<RoutePlan> findByShipmentIdAndStatus(Long shipmentId, RoutePlanStatus status);
    Optional<RoutePlan> findFirstByShipmentIdAndStatusOrderByIdDesc(Long shipmentId, RoutePlanStatus status);
}
