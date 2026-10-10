package com.nerlogix.dto;

import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.SegmentStatus;
import java.time.LocalDateTime;

public record RoadSegmentDto(
    Long id,
    String name,
    String highwayCode,
    AssetType assetType,
    Double lengthKm,
    Double baseSpeedKmph,
    Integer terrainClass,
    Boolean landslideProne,
    Boolean floodProne,
    SegmentStatus status,
    Double riskScore,
    String statusReason,
    String polyline,
    String fromNodeName,
    String toNodeName,
    String state,
    LocalDateTime updatedAt
) {}
