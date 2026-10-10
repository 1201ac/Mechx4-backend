package com.nerlogix.dto;

import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.NodeKind;
import com.nerlogix.model.enums.SegmentStatus;
import java.util.List;

public record PublicNetworkResponse(
    List<NetworkNodeDto> nodes,
    List<NetworkSegmentDto> segments
) {
    public record NetworkNodeDto(
        Long id,
        String name,
        Double lat,
        Double lng,
        NodeKind kind
    ) {}

    public record NetworkSegmentDto(
        Long id,
        Long fromNodeId,
        Long toNodeId,
        String name,
        String highwayCode,
        AssetType assetType,
        Double lengthKm,
        SegmentStatus status,
        Double riskScore,
        String polyline
    ) {}
}
