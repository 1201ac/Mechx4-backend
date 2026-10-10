package com.nerlogix.dto;

import com.nerlogix.model.enums.WeatherCondition;
import com.nerlogix.model.enums.WeatherSource;
import java.time.LocalDateTime;

public record MapWeatherDto(
    Long districtId,
    String districtName,
    WeatherCondition condition,
    Double rainMm,
    String note,
    WeatherSource source,
    LocalDateTime loggedAt
) {}
