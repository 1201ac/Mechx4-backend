package com.nerlogix.controller;

import com.nerlogix.dto.PublicNetworkResponse;
import com.nerlogix.dto.PublicOverviewResponse;
import com.nerlogix.model.enums.AlertStatus;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.model.enums.ShipmentStatus;
import com.nerlogix.repository.*;
import com.nerlogix.service.SystemCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/public", "/public"})
public class PublicController {

    private final DistrictRepository districtRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final ShipmentRepository shipmentRepository;
    private final AlertRepository alertRepository;
    private final SystemCacheService systemCacheService;

    public PublicController(
            DistrictRepository districtRepository,
            RoadNodeRepository roadNodeRepository,
            RoadSegmentRepository roadSegmentRepository,
            ShipmentRepository shipmentRepository,
            AlertRepository alertRepository,
            SystemCacheService systemCacheService) {
        this.districtRepository = districtRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.shipmentRepository = shipmentRepository;
        this.alertRepository = alertRepository;
        this.systemCacheService = systemCacheService;
    }

    @GetMapping("/network")
    public ResponseEntity<PublicNetworkResponse> getNetwork() {
        List<PublicNetworkResponse.NetworkNodeDto> nodes = roadNodeRepository.findAll().stream()
                .map(n -> new PublicNetworkResponse.NetworkNodeDto(n.getId(), n.getName(), n.getLat(), n.getLng(), n.getKind()))
                .toList();

        List<PublicNetworkResponse.NetworkSegmentDto> segments = roadSegmentRepository.findAll().stream()
                .map(s -> new PublicNetworkResponse.NetworkSegmentDto(
                        s.getId(),
                        s.getFromNode().getId(),
                        s.getToNode().getId(),
                        s.getName(),
                        s.getHighwayCode(),
                        s.getAssetType(),
                        s.getLengthKm(),
                        s.getStatus(),
                        s.getRiskScore(),
                        s.getPolyline()
                ))
                .toList();

        return ResponseEntity.ok(new PublicNetworkResponse(nodes, segments));
    }

    @GetMapping("/overview")
    public ResponseEntity<PublicOverviewResponse> getOverview() {
        PublicOverviewResponse response = systemCacheService.getOrCompute("public.overview", () -> {
            long districtsCount = districtRepository.count();
            long segmentsCount = roadSegmentRepository.count();
            long movingCount = shipmentRepository.findByStatus(ShipmentStatus.IN_TRANSIT).size();
            long disruptionsCount = roadSegmentRepository.findByStatus(SegmentStatus.BLOCKED).size()
                    + roadSegmentRepository.findByStatus(SegmentStatus.RESTRICTED).size();
            return new PublicOverviewResponse(districtsCount, segmentsCount, movingCount, disruptionsCount);
        });

        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<java.util.Map<String, String>> getConfig() {
        String apiKey = System.getenv().getOrDefault("VITE_GOOGLE_MAPS_API_KEY", System.getenv().getOrDefault("GOOGLE_MAPS_API_KEY", ""));
        String mapId = System.getenv().getOrDefault("VITE_GOOGLE_MAPS_MAP_ID", System.getenv().getOrDefault("GOOGLE_MAPS_MAP_ID", ""));
        return ResponseEntity.ok(java.util.Map.of(
            "googleMapsApiKey", apiKey,
            "googleMapsMapId", mapId
        ));
    }
}
