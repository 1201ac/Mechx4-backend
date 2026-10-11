package com.nerlogix.dto;

import java.util.List;

public record DriverCandidateDto(
    Long driverUserId,
    String driverName,
    String driverEmail,
    Long vehicleId,
    String vehicleRegNo,
    String vehicleType,
    Double vehicleCapacityKg,
    Double distanceToOriginKm,
    Integer matchScore,
    List<String> matchReasons
) {}
