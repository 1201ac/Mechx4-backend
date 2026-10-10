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
    boolean isEmergencyOptimized
) {}
