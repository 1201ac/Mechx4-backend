package com.nerlogix.dto;

import com.nerlogix.model.enums.WeatherCondition;
import jakarta.validation.constraints.NotNull;

public record DemoRainRequest(
    @NotNull(message = "District ID is required")
    Long districtId,

    @NotNull(message = "Condition is required")
    WeatherCondition condition
) {}
