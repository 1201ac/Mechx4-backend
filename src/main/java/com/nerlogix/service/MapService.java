package com.nerlogix.service;

import com.nerlogix.dto.*;
import com.nerlogix.entity.*;
import com.nerlogix.model.enums.IncidentStatus;
import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MapService {

    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final VehicleRepository vehicleRepository;
    private final IncidentRepository incidentRepository;
    private final DistrictRepository districtRepository;
    private final WeatherLogRepository weatherLogRepository;
    private final SystemCacheService systemCacheService;

    public MapService(
            RoadSegmentRepository roadSegmentRepository,
            RoadNodeRepository roadNodeRepository,
            VehicleRepository vehicleRepository,
            IncidentRepository incidentRepository,
            DistrictRepository districtRepository,
            WeatherLogRepository weatherLogRepository,
            SystemCacheService systemCacheService) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.vehicleRepository = vehicleRepository;
        this.incidentRepository = incidentRepository;
        this.districtRepository = districtRepository;
        this.weatherLogRepository = weatherLogRepository;
        this.systemCacheService = systemCacheService;
    }

    @Transactional(readOnly = true)
    public List<MapSegmentDto> getSegments(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<MapSegmentDto> allSegments = systemCacheService.getOrCompute("map.segments", this::loadAllMapSegments);
        if (minLat == null && maxLat == null && minLng == null && maxLng == null) {
            return allSegments;
        }
        return allSegments.stream()
                .filter(s -> inBbox(s.fromLat(), s.fromLng(), minLat, maxLat, minLng, maxLng) ||
                             inBbox(s.toLat(), s.toLng(), minLat, maxLat, minLng, maxLng))
                .toList();
    }

    private List<MapSegmentDto> loadAllMapSegments() {
        return roadSegmentRepository.findAll().stream()
                .map(s -> new MapSegmentDto(
                        s.getId(),
                        s.getName(),
                        s.getHighwayCode(),
                        s.getAssetType(),
                        s.getStatus(),
                        s.getRiskScore(),
                        s.getTerrainClass(),
                        s.getLandslideProne(),
                        s.getFloodProne(),
                        s.getStatusReason(),
                        s.getPolyline(),
                        s.getFromNode().getId(),
                        s.getFromNode().getName(),
                        s.getFromNode().getLat(),
                        s.getFromNode().getLng(),
                        s.getToNode().getId(),
                        s.getToNode().getName(),
                        s.getToNode().getLat(),
                        s.getToNode().getLng()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapVehicleDto> getVehicles() {
        return vehicleRepository.findAll().stream()
                .map(v -> new MapVehicleDto(
                        v.getId(),
                        v.getRegNo(),
                        v.getType(),
                        v.getStatus(),
                        v.getLat(),
                        v.getLng(),
                        v.getHeading(),
                        v.getSpeedKmph(),
                        null,
                        null,
                        null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapIncidentDto> getIncidents() {
        return incidentRepository.findAll().stream()
                .map(inc -> new MapIncidentDto(
                        inc.getId(),
                        inc.getType(),
                        inc.getSeverity(),
                        inc.getLat(),
                        inc.getLng(),
                        inc.getSegment() != null ? inc.getSegment().getId() : null,
                        inc.getDistrict() != null ? inc.getDistrict().getId() : null,
                        inc.getDescription(),
                        inc.getStatus(),
                        inc.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapDistrictDto> getDistricts() {
        return districtRepository.findAll().stream()
                .map(d -> {
                    // Placeholder connectivity calculation until RiskEngine full pass
                    double connectivity = 100.0;
                    String status = "GOOD";
                    return new MapDistrictDto(
                            d.getId(),
                            d.getName(),
                            d.getState(),
                            d.getLat(),
                            d.getLng(),
                            d.getPopulationBand(),
                            connectivity,
                            status
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoadNodeDto> getLocations() {
        return roadNodeRepository.findAll().stream()
                .map(n -> new RoadNodeDto(
                        n.getId(),
                        n.getName(),
                        n.getLat(),
                        n.getLng(),
                        n.getDistrict() != null ? n.getDistrict().getId() : null,
                        n.getDistrict() != null ? n.getDistrict().getName() : null,
                        n.getKind()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MapWeatherDto> getWeather() {
        return weatherLogRepository.findAll().stream()
                .map(w -> new MapWeatherDto(
                        w.getDistrict().getId(),
                        w.getDistrict().getName(),
                        w.getCondition(),
                        w.getRainMm(),
                        w.getNote(),
                        w.getSource(),
                        w.getLoggedAt()
                ))
                .toList();
    }

    private boolean inBbox(double lat, double lng, Double minLat, Double maxLat, Double minLng, Double maxLng) {
        if (minLat != null && lat < minLat) return false;
        if (maxLat != null && lat > maxLat) return false;
        if (minLng != null && lng < minLng) return false;
        if (maxLng != null && lng > maxLng) return false;
        return true;
    }
}
