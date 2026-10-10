package com.nerlogix.dto;

import com.nerlogix.model.enums.SegmentStatus;
import java.time.LocalDateTime;

public record RoadStatusHistoryDto(
    Long id,
    Long segmentId,
    SegmentStatus oldStatus,
    SegmentStatus newStatus,
    String reason,
    String changedByName,
    LocalDateTime changedAt
) {}
