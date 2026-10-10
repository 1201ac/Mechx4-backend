package com.nerlogix.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user
) {}
