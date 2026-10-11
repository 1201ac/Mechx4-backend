package com.nerlogix.dto;

import java.util.List;

public record DriverListItemDto(
    Long id,
    String name,
    String email,
    Boolean active,
    List<String> vehicleRegNos,
    String vehicleType,
    Double vehicleCapacityKg,
    String currentJob
) {}
