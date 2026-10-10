package com.nerlogix.dto;

public record UpdateProfileRequest(
    String name,
    String language,
    String alertPreferences
) {}
