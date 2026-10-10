package com.nerlogix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDistrictRequest(
    @NotBlank String name,
    @NotBlank String state,
    @NotNull Double lat,
    @NotNull Double lng,
    @NotBlank String populationBand
) {}
