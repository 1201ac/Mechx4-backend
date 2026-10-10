package com.nerlogix.dto;

import com.nerlogix.model.enums.SegmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoadStatusUpdateRequest(
    @NotNull(message = "Status is required")
    SegmentStatus status,

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    String reason
) {}
