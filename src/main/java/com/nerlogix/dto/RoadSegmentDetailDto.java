package com.nerlogix.dto;

import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.SegmentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record RoadSegmentDetailDto(
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
    Long fromNodeId,
    String fromNodeName,
    double fromLat,
    double fromLng,
    Long toNodeId,
    String toNodeName,
    double toLat,
    double toLng,
    String state,
    List<RiskFactorDto> riskFactors,
    List<RoadStatusHistoryDto> history,
    LocalDateTime updatedAt
) {}
