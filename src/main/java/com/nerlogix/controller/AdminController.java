package com.nerlogix.controller;

import com.nerlogix.dto.DemoRainRequest;
import com.nerlogix.dto.DistrictWeatherDto;
import com.nerlogix.dto.UserDto;
import com.nerlogix.entity.AuditLog;
import com.nerlogix.entity.RuleConfig;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.Role;
import com.nerlogix.service.AdminService;
import com.nerlogix.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final WeatherService weatherService;

    public AdminController(AdminService adminService, WeatherService weatherService) {
        this.adminService = adminService;
        this.weatherService = weatherService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(adminService.getUsers(pageable));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Role role = Role.valueOf(body.get("role"));
        return ResponseEntity.ok(adminService.updateUserRole(id, role, currentUser));
    }

    @PatchMapping("/users/{id}/active")
    public ResponseEntity<UserDto> updateUserActive(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(adminService.updateUserActive(id, active, currentUser));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.resetUserPassword(id, body.get("password"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<RuleConfig>> getRules() {
        return ResponseEntity.ok(adminService.getRuleConfigs());
    }

    @PatchMapping("/rules/{key}")
    public ResponseEntity<RuleConfig> updateRule(@PathVariable String key, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateRuleConfig(key, body.get("value")));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<Map<String, Object>>> getApiKeys() {
        return ResponseEntity.ok(adminService.getApiKeys());
    }

    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, String>> createApiKey(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.createApiKey(body.get("name"), currentUser));
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        adminService.deleteApiKey(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/demo/rain")
    public ResponseEntity<DistrictWeatherDto> triggerDemoRain(@Valid @RequestBody DemoRainRequest request) {
        DistrictWeatherDto dto = weatherService.triggerDemoRain(request.districtId(), request.condition());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/demo/reset")
    public ResponseEntity<Void> resetDemoData() {
        adminService.resetDemoData();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/roads/snap-geometry")
    public ResponseEntity<com.nerlogix.dto.RoadSegmentDto> snapRoadGeometry(@RequestBody Map<String, Long> body) {
        Long segmentId = body.get("segmentId");
        return ResponseEntity.ok(adminService.snapRoadGeometry(segmentId));
    }

    @GetMapping("/districts")
    public ResponseEntity<List<com.nerlogix.entity.District>> getAdminDistricts() {
        return ResponseEntity.ok(adminService.getAllDistricts());
    }

    @PutMapping("/districts/{id}")
    public ResponseEntity<com.nerlogix.entity.District> updateAdminDistrict(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody com.nerlogix.dto.UpdateDistrictRequest request) {
        return ResponseEntity.ok(adminService.updateDistrict(id, request, currentUser));
    }

    @GetMapping("/roads")
    public ResponseEntity<List<com.nerlogix.entity.RoadSegment>> getAdminRoads() {
        return ResponseEntity.ok(adminService.getAllRoads());
    }

    @PutMapping("/roads/{id}")
    public ResponseEntity<com.nerlogix.entity.RoadSegment> updateAdminRoadSegment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody com.nerlogix.dto.UpdateRoadSegmentRequest request) {
        return ResponseEntity.ok(adminService.updateRoadSegment(id, request, currentUser));
    }
}
