package com.nerlogix.dto;

import java.util.Map;

public record BroadcastNotificationRequest(
    Map<String, String> titles,
    Map<String, String> messages
) {}
