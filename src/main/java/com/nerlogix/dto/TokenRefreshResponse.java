package com.nerlogix.dto;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken
) {}
