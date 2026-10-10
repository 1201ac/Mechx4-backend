package com.nerlogix.dto;

import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.SegmentStatus;

public record MapSegmentDto(
    Long id,
    String name,
    String highwayCode,
    AssetType assetType,
    SegmentStatus status,
    Double riskScore,
    Integer terrainClass,
    Boolean landslideProne,
    Boolean floodProne,
    String statusReason,
    String polyline,
    Long fromNodeId,
    String fromNodeName,
    double fromLat,
    double fromLng,
    Long toNodeId,
    String toNodeName,
    double toLat,
    double toLng
) {}
