package com.nerlogix.controller;

import com.nerlogix.dto.DistrictWeatherDto;
import com.nerlogix.dto.WeatherLogRequest;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.WeatherSource;
import com.nerlogix.service.WeatherService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/districts")
    public List<DistrictWeatherDto> getDistrictWeatherList() {
        return weatherService.getDistrictWeatherList();
    }

    @GetMapping("/logs")
    public org.springframework.data.domain.Page<DistrictWeatherDto> getWeatherLogs(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return weatherService.getWeatherLogs(pageable);
    }

    @PostMapping("/logs")
    public DistrictWeatherDto logWeather(
            @Valid @RequestBody WeatherLogRequest request,
            @AuthenticationPrincipal User currentUser) {
        return weatherService.addWeatherLog(request, currentUser, WeatherSource.MANUAL);
    }
}
