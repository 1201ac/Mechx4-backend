package com.nerlogix.dto;

import com.nerlogix.model.enums.AssetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateRoadSegmentRequest(
    @NotBlank String name,
    @NotNull AssetType assetType,
    @NotNull @Positive Double baseSpeedKmph,
    @NotNull @Min(1) @Max(5) Integer terrainClass,
    @NotNull Boolean landslideProne,
    @NotNull Boolean floodProne
) {}
