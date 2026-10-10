package com.nerlogix.dto;

import com.nerlogix.model.enums.CargoCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateDistrictStockRequest(
    @NotNull CargoCategory category,
    @NotNull @PositiveOrZero Double stockUnits,
    @NotNull @Positive Double dailyDemandUnits
) {}
