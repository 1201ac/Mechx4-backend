package com.nerlogix.dto;

public record PublicOverviewResponse(
    long districtsMonitored,
    long segmentsTracked,
    long shipmentsMoving,
    long disruptionsOpen
) {}
