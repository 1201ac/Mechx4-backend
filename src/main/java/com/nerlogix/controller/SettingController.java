package com.nerlogix.controller;

import com.nerlogix.entity.User;
import com.nerlogix.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/emergency")
    public ResponseEntity<Map<String, Object>> getEmergencyMode() {
        return ResponseEntity.ok(settingService.getEmergencyMode());
    }

    @PostMapping("/emergency")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> setEmergencyMode(
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal User currentUser) {
        boolean enabled = body != null && Boolean.TRUE.equals(body.get("enabled"));
        return ResponseEntity.ok(settingService.setEmergencyMode(enabled, currentUser));
    }
}
