package com.nerlogix.service;

import com.nerlogix.dto.*;
import com.nerlogix.entity.*;
import com.nerlogix.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final RoadNodeRepository roadNodeRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final DistrictRepository districtRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentRepository shipmentRepository;
    private final IncidentRepository incidentRepository;

    public SearchService(
            RoadNodeRepository roadNodeRepository,
            RoadSegmentRepository roadSegmentRepository,
            DistrictRepository districtRepository,
            VehicleRepository vehicleRepository,
            ShipmentRepository shipmentRepository,
            IncidentRepository incidentRepository) {
        this.roadNodeRepository = roadNodeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.districtRepository = districtRepository;
        this.vehicleRepository = vehicleRepository;
        this.shipmentRepository = shipmentRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String query) {
        Map<String, Object> results = new HashMap<>();
        if (query == null || query.isBlank()) {
            results.put("nodes", List.of());
            results.put("segments", List.of());
            results.put("districts", List.of());
            results.put("vehicles", List.of());
            results.put("shipments", List.of());
            results.put("incidents", List.of());
            return results;
        }

        String q = query.trim().toLowerCase();

        List<RoadNode> nodes = roadNodeRepository.findAll().stream()
                .filter(n -> n.getName().toLowerCase().contains(q))
                .limit(10)
                .toList();

        List<RoadSegment> segments = roadSegmentRepository.findAll().stream()
                .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                        (s.getHighwayCode() != null && s.getHighwayCode().toLowerCase().contains(q)))
                .limit(10)
                .toList();

        List<District> districts = districtRepository.findAll().stream()
                .filter(d -> d.getName().toLowerCase().contains(q) || d.getState().toLowerCase().contains(q))
                .limit(10)
                .toList();

        List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                .filter(v -> v.getRegNo().toLowerCase().contains(q) || v.getType().name().toLowerCase().contains(q))
                .limit(10)
                .toList();

        List<Shipment> shipments = shipmentRepository.findAll().stream()
                .filter(sh -> sh.getCode().toLowerCase().contains(q) ||
                        (sh.getCargoDescription() != null && sh.getCargoDescription().toLowerCase().contains(q)) ||
                        sh.getCargoCategory().name().toLowerCase().contains(q))
                .limit(10)
                .toList();

        List<Incident> incidents = incidentRepository.findAll().stream()
                .filter(inc -> (inc.getDescription() != null && inc.getDescription().toLowerCase().contains(q)) ||
                        inc.getType().name().toLowerCase().contains(q))
                .limit(10)
                .toList();

        results.put("nodes", nodes.stream().map(n -> Map.of("id", n.getId(), "name", n.getName(), "kind", n.getKind())).toList());
        results.put("segments", segments.stream().map(s -> Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "highwayCode", s.getHighwayCode() != null ? s.getHighwayCode() : "",
                "assetType", s.getAssetType(),
                "status", s.getStatus(),
                "riskScore", s.getRiskScore() != null ? s.getRiskScore() : 0.0
        )).toList());
        results.put("districts", districts.stream().map(d -> Map.of(
                "id", d.getId(),
                "name", d.getName(),
                "state", d.getState(),
                "connectivityScore", d.getConnectivityScore() != null ? d.getConnectivityScore() : 100.0
        )).toList());
        results.put("vehicles", vehicles.stream().map(VehicleDto::fromEntity).toList());
        results.put("shipments", shipments.stream().map(ShipmentDto::fromEntity).toList());
        results.put("incidents", incidents.stream().map(IncidentDto::fromEntity).toList());

        return results;
    }
}
