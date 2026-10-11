package com.nerlogix.dto;

import java.util.List;

public record RouteOptionDto(
    int optionIndex,
    String name,
    double distanceKm,
    int etaMinutes,
    double riskScore,
    String riskCategory,
    int expectedDelayMin,
    String reason,
    String geometry,
    List<Long> segmentIds,
    List<String> segmentNames,
    boolean isEmergencyOptimized,
    List<Long> nodeIds,
    String label,
    List<String> highlights
) {
    public RouteOptionDto(
        int optionIndex,
        String name,
        double distanceKm,
        int etaMinutes,
        double riskScore,
        String riskCategory,
        int expectedDelayMin,
        String reason,
        String geometry,
        List<Long> segmentIds,
        List<String> segmentNames,
        boolean isEmergencyOptimized
    ) {
        this(
            optionIndex,
            name,
            distanceKm,
            etaMinutes,
            riskScore,
            riskCategory,
            expectedDelayMin,
            reason,
            geometry,
            segmentIds,
            segmentNames,
            isEmergencyOptimized,
            List.of(),
            optionIndex == 1 ? "Fastest" : (optionIndex == 2 ? "Safest" : "Balanced"),
            List.of()
        );
    }
}
