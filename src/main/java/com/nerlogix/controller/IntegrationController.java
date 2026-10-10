package com.nerlogix.controller;

import com.nerlogix.dto.DistrictWeatherDto;
import com.nerlogix.dto.RoadSegmentDto;
import com.nerlogix.dto.VehicleDto;
import com.nerlogix.entity.ApiKey;
import com.nerlogix.entity.User;

import com.nerlogix.model.enums.WeatherCondition;
import com.nerlogix.model.enums.WeatherSource;
import com.nerlogix.repository.ApiKeyRepository;
import com.nerlogix.repository.UserRepository;

import com.nerlogix.service.RoadService;
import com.nerlogix.service.VehicleService;
import com.nerlogix.service.WeatherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final RoadService roadService;
    private final WeatherService weatherService;
    private final VehicleService vehicleService;

    public IntegrationController(
            ApiKeyRepository apiKeyRepository,
            UserRepository userRepository,
            RoadService roadService,
            WeatherService weatherService,
            VehicleService vehicleService) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.roadService = roadService;
        this.weatherService = weatherService;
        this.vehicleService = vehicleService;
    }

    private void validateApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-API-Key header");
        }
        String hash = hashKey(rawApiKey);
        ApiKey key = apiKeyRepository.findByKeyHashAndActiveTrue(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid or inactive API key"));

        key.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(key);
    }

    @PostMapping("/road-status")
    public ResponseEntity<RoadSegmentDto> ingestRoadStatus(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> payload) {

        validateApiKey(apiKey);

        Long segmentId = Long.valueOf(payload.get("segmentId").toString());
        com.nerlogix.model.enums.SegmentStatus status = com.nerlogix.model.enums.SegmentStatus.valueOf(payload.get("status").toString());
        String reason = payload.getOrDefault("reason", "Integration update").toString();

        User sysUser = userRepository.findByEmail("admin@nerlogix.com").orElse(null);

        RoadSegmentDto dto = roadService.updateRoadStatus(segmentId, status, reason, sysUser, "127.0.0.1");
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/weather")
    public ResponseEntity<DistrictWeatherDto> ingestWeather(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> payload) {

        validateApiKey(apiKey);

        Long districtId = Long.valueOf(payload.get("districtId").toString());
        WeatherCondition condition = WeatherCondition.valueOf(payload.get("condition").toString());
        Double rainMm = payload.get("rainMm") != null ? Double.valueOf(payload.get("rainMm").toString()) : 0.0;
        String note = payload.getOrDefault("note", "External sensor update").toString();

        User sysUser = userRepository.findByEmail("admin@nerlogix.com").orElse(null);

        com.nerlogix.dto.WeatherLogRequest req = new com.nerlogix.dto.WeatherLogRequest(districtId, condition, rainMm, note);
        DistrictWeatherDto dto = weatherService.addWeatherLog(req, sysUser, WeatherSource.MANUAL);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/assets")
    public ResponseEntity<Void> ingestAssets(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody List<Map<String, Object>> assetPositions) {

        validateApiKey(apiKey);

        User sysUser = userRepository.findByEmail("admin@nerlogix.com").orElse(null);
        if (sysUser == null) return ResponseEntity.ok().build();

        List<com.nerlogix.dto.VehiclePositionRequest> batch = assetPositions.stream().map(pos -> {
            Double lat = Double.valueOf(pos.get("lat").toString());
            Double lng = Double.valueOf(pos.get("lng").toString());
            Double heading = pos.get("heading") != null ? Double.valueOf(pos.get("heading").toString()) : 0.0;
            Double speedKmph = pos.get("speedKmph") != null ? Double.valueOf(pos.get("speedKmph").toString()) : 40.0;
            return new com.nerlogix.dto.VehiclePositionRequest(lat, lng, heading, speedKmph, LocalDateTime.now());
        }).toList();

        vehicleService.recordDriverPositions(sysUser, batch);
        return ResponseEntity.ok().build();
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
