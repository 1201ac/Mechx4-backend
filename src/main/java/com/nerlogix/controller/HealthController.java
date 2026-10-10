package com.nerlogix.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                health.put("database", "UP");
            } else {
                health.put("database", "DOWN");
            }
        } catch (Exception e) {
            health.put("database", "DOWN (" + e.getMessage() + ")");
        }
        return ResponseEntity.ok(health);
    }
}
