package com.nerlogix.dto;

import com.nerlogix.entity.RoutePlan;
import com.nerlogix.model.enums.RoutePlanStatus;

import java.time.LocalDateTime;

public record RoutePlanDto(
        Long id,
        Long shipmentId,
        String segmentIds,
        String geometry,
        Double distanceKm,
        Double etaMin,
        Double riskScore,
        RoutePlanStatus status,
        String reason,
        LocalDateTime createdAt
) {
    public static RoutePlanDto fromEntity(RoutePlan entity) {
        if (entity == null) return null;
        return new RoutePlanDto(
                entity.getId(),
                entity.getShipment().getId(),
                entity.getSegmentIds(),
                entity.getGeometry(),
                entity.getDistanceKm(),
                entity.getEtaMin(),
                entity.getRiskScore(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
