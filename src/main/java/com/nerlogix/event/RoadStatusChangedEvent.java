package com.nerlogix.event;

import com.nerlogix.model.enums.SegmentStatus;

public record RoadStatusChangedEvent(
    Long segmentId,
    String segmentName,
    SegmentStatus oldStatus,
    SegmentStatus newStatus,
    String reason
) {}
