package com.nerlogix.controller;

import com.nerlogix.dto.*;
import com.nerlogix.service.MapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/segments")
    public List<MapSegmentDto> getSegments(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng) {
        return mapService.getSegments(minLat, maxLat, minLng, maxLng);
    }

    @GetMapping("/vehicles")
    public List<MapVehicleDto> getVehicles() {
        return mapService.getVehicles();
    }

    @GetMapping("/incidents")
    public List<MapIncidentDto> getIncidents() {
        return mapService.getIncidents();
    }

    @GetMapping("/districts")
    public List<MapDistrictDto> getDistricts() {
        return mapService.getDistricts();
    }

    @GetMapping("/locations")
    public List<RoadNodeDto> getLocations() {
        return mapService.getLocations();
    }

    @GetMapping("/weather")
    public List<MapWeatherDto> getWeather() {
        return mapService.getWeather();
    }
}
