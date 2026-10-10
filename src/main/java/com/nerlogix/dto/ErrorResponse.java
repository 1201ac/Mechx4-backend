package com.nerlogix.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now().toString(), status, error, message, path, fieldErrors);
    }
}
