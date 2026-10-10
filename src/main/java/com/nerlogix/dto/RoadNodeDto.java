package com.nerlogix.dto;

import com.nerlogix.entity.RoadNode;
import com.nerlogix.model.enums.NodeKind;

public record RoadNodeDto(
    Long id,
    String name,
    double lat,
    double lng,
    Long districtId,
    String districtName,
    NodeKind kind
) {
    public static RoadNodeDto fromEntity(RoadNode n) {
        if (n == null) return null;
        return new RoadNodeDto(
            n.getId(),
            n.getName(),
            n.getLat(),
            n.getLng(),
            n.getDistrict() != null ? n.getDistrict().getId() : null,
            n.getDistrict() != null ? n.getDistrict().getName() : null,
            n.getKind()
        );
    }
}
