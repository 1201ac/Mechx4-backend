package com.nerlogix.controller;

import com.nerlogix.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/api/dashboard/districts")
    public ResponseEntity<List<Map<String, Object>>> getDistrictConnectivity() {
        return ResponseEntity.ok(dashboardService.getDistrictConnectivityList());
    }

    @GetMapping("/api/dashboard/supply")
    public ResponseEntity<List<Map<String, Object>>> getSupplyStatus() {
        return ResponseEntity.ok(dashboardService.getSupplyStatus());
    }

    @GetMapping("/api/dashboard/movement")
    public ResponseEntity<Map<String, Object>> getMovementStatus() {
        return ResponseEntity.ok(dashboardService.getMovementStatus());
    }

    @GetMapping("/api/dashboard/emergency")
    public ResponseEntity<Map<String, Object>> getEmergencyDashboard() {
        return ResponseEntity.ok(dashboardService.getEmergencyDashboard());
    }

    @GetMapping("/api/analytics/charts")
    public ResponseEntity<Map<String, Object>> getAnalyticsCharts() {
        return ResponseEntity.ok(dashboardService.getAnalyticsCharts());
    }
}
