package com.nerlogix.dto;

public record MapDistrictDto(
    Long id,
    String name,
    String state,
    double lat,
    double lng,
    String populationBand,
    double connectivityScore,
    String status
) {}
