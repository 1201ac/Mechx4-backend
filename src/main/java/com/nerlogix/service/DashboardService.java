package com.nerlogix.service;

import com.nerlogix.dto.*;
import com.nerlogix.entity.*;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DashboardService {

    private final DistrictRepository districtRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final VehicleRepository vehicleRepository;
    private final ShipmentRepository shipmentRepository;
    private final FieldReportRepository fieldReportRepository;
    private final AlertRepository alertRepository;
    private final DistrictStockRepository districtStockRepository;
    private final SettingRepository settingRepository;
    private final IncidentRepository incidentRepository;
    private final SystemCacheService systemCacheService;

    public DashboardService(
            DistrictRepository districtRepository,
            RoadSegmentRepository roadSegmentRepository,
            VehicleRepository vehicleRepository,
            ShipmentRepository shipmentRepository,
            FieldReportRepository fieldReportRepository,
            AlertRepository alertRepository,
            DistrictStockRepository districtStockRepository,
            SettingRepository settingRepository,
            IncidentRepository incidentRepository,
            SystemCacheService systemCacheService) {
        this.districtRepository = districtRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.shipmentRepository = shipmentRepository;
        this.fieldReportRepository = fieldReportRepository;
        this.alertRepository = alertRepository;
        this.districtStockRepository = districtStockRepository;
        this.settingRepository = settingRepository;
        this.incidentRepository = incidentRepository;
        this.systemCacheService = systemCacheService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        return systemCacheService.getOrCompute("dashboard.summary", this::computeSummary);
    }

    private Map<String, Object> computeSummary() {
        List<District> districts = districtRepository.findAll();
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();
        Map<Long, List<RoadSegment>> segmentsByDistrict = groupSegmentsByDistrict(allSegments);

        long totalDistricts = districts.size();

        long degradedCount = districts.stream()
                .filter(d -> calculateDistrictConnectivity(d, segmentsByDistrict) < 75.0)
                .count();

        long activeVehicles = vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.ON_TRIP)
                .count();

        long activeShipments = shipmentRepository.findAll().stream()
                .filter(s -> s.getStatus() == ShipmentStatus.IN_TRANSIT || s.getStatus() == ShipmentStatus.DELAYED)
                .count();

        long pendingFieldReports = fieldReportRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReportStatus.PENDING)
                .count();

        long criticalAlerts = alertRepository.findAll().stream()
                .filter(a -> a.getStatus() != AlertStatus.RESOLVED && a.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        // Supply gaps: stockUnits / dailyDemandUnits < 3
        long supplyGaps = districtStockRepository.findAll().stream()
                .filter(s -> s.getDailyDemandUnits() > 0 && (s.getStockUnits() / s.getDailyDemandUnits()) < 3.0)
                .count();

        boolean emergencyMode = settingRepository.findBySettingKey("emergency_mode")
                .map(Setting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(false);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDistricts", totalDistricts);
        summary.put("degradedOrInaccessibleDistricts", degradedCount);
        summary.put("activeVehicles", activeVehicles);
        summary.put("activeShipments", activeShipments);
        summary.put("pendingFieldReports", pendingFieldReports);
        summary.put("criticalAlerts", criticalAlerts);
        summary.put("supplyGaps", supplyGaps);
        summary.put("emergencyMode", emergencyMode);

        return summary;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDistrictConnectivityList() {
        return systemCacheService.getOrCompute("dashboard.districts", this::computeDistrictConnectivityList);
    }

    private List<Map<String, Object>> computeDistrictConnectivityList() {
        List<District> districts = districtRepository.findAll();
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();
        Map<Long, List<RoadSegment>> segmentsByDistrict = groupSegmentsByDistrict(allSegments);

        List<Map<String, Object>> result = new ArrayList<>();

        for (District d : districts) {
            double score = calculateDistrictConnectivity(d, segmentsByDistrict);
            String category = score >= 75.0 ? "GOOD" : score >= 40.0 ? "DEGRADED" : "POOR";
            if (score <= 0.0) category = "INACCESSIBLE";

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", d.getId());
            item.put("name", d.getName());
            item.put("state", d.getState());
            item.put("connectivityScore", Math.round(score * 10.0) / 10.0);
            item.put("category", category);
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSupplyStatus() {
        return systemCacheService.getOrCompute("dashboard.supply", this::computeSupplyStatus);
    }

    private List<Map<String, Object>> computeSupplyStatus() {
        List<DistrictStock> stocks = districtStockRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (DistrictStock s : stocks) {
            double daysCover = s.getDailyDemandUnits() > 0 ? (s.getStockUnits() / s.getDailyDemandUnits()) : 999.0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("districtName", s.getDistrict().getName());
            item.put("state", s.getDistrict().getState());
            item.put("category", s.getCategory().name());
            item.put("stockUnits", Math.round(s.getStockUnits() * 10.0) / 10.0);
            item.put("dailyDemandUnits", Math.round(s.getDailyDemandUnits() * 10.0) / 10.0);
            item.put("daysOfCover", Math.round(daysCover * 10.0) / 10.0);
            item.put("isSupplyGap", daysCover < 3.0);
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsCharts() {
        return systemCacheService.getOrCompute("dashboard.charts", this::computeAnalyticsCharts);
    }

    private Map<String, Object> computeAnalyticsCharts() {
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        long openCount = segments.stream().filter(s -> s.getStatus() == SegmentStatus.OPEN).count();
        long restrictedCount = segments.stream().filter(s -> s.getStatus() == SegmentStatus.RESTRICTED).count();
        long blockedCount = segments.stream().filter(s -> s.getStatus() == SegmentStatus.BLOCKED).count();

        List<Map<String, Object>> roadStatusData = List.of(
                Map.of("name", "OPEN", "value", openCount),
                Map.of("name", "RESTRICTED", "value", restrictedCount),
                Map.of("name", "BLOCKED", "value", blockedCount)
        );

        List<Incident> incidents = incidentRepository.findAll();
        Map<String, Long> incidentTypeCounts = new HashMap<>();
        for (Incident i : incidents) {
            incidentTypeCounts.put(i.getType().name(), incidentTypeCounts.getOrDefault(i.getType().name(), 0L) + 1);
        }
        List<Map<String, Object>> incidentChartData = incidentTypeCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("type", e.getKey(), "count", e.getValue()))
                .toList();

        List<Shipment> shipments = shipmentRepository.findAll();
        Map<String, Long> cargoCategoryCounts = new HashMap<>();
        for (Shipment s : shipments) {
            cargoCategoryCounts.put(s.getCargoCategory().name(), cargoCategoryCounts.getOrDefault(s.getCargoCategory().name(), 0L) + 1);
        }
        List<Map<String, Object>> cargoChartData = cargoCategoryCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("category", e.getKey(), "count", e.getValue()))
                .toList();

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("roadStatusBreakdown", roadStatusData);
        charts.put("incidentsByType", incidentChartData);
        charts.put("cargoCategoryDistribution", cargoChartData);

        return charts;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMovementStatus() {
        return systemCacheService.getOrCompute("dashboard.movement", 10_000L, this::computeMovementStatus);
    }

    private Map<String, Object> computeMovementStatus() {
        List<Shipment> inTransit = shipmentRepository.findByStatusIn(List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED));
        List<Vehicle> activeVehicles = vehicleRepository.findByStatus(VehicleStatus.ON_TRIP);

        List<Map<String, Object>> shipmentMovements = inTransit.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("shipmentCode", s.getCode());
            map.put("category", s.getCargoCategory().name());
            map.put("priority", s.getPriority().name());
            map.put("progressPct", s.getProgressPct());
            map.put("delayMin", s.getDelayMin());
            map.put("vehicleRegNo", s.getVehicle() != null ? s.getVehicle().getRegNo() : null);
            map.put("vehicleSpeedKmph", s.getVehicle() != null ? s.getVehicle().getSpeedKmph() : 0.0);
            map.put("origin", s.getOriginNode() != null ? s.getOriginNode().getName() : null);
            map.put("destination", s.getDestinationNode() != null ? s.getDestinationNode().getName() : null);
            return map;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalActiveShipments", inTransit.size());
        res.put("totalActiveVehicles", activeVehicles.size());
        res.put("shipments", shipmentMovements);
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmergencyDashboard() {
        return systemCacheService.getOrCompute("dashboard.emergency", this::computeEmergencyDashboard);
    }

    private Map<String, Object> computeEmergencyDashboard() {
        boolean emergencyMode = settingRepository.findBySettingKey("emergency_mode")
                .map(Setting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(false);

        List<ShipmentDto> emergencyShipments = shipmentRepository.findAll().stream()
                .filter(s -> s.getPriority() == ShipmentPriority.EMERGENCY)
                .map(ShipmentDto::fromEntity)
                .toList();

        List<AlertDto> criticalAlerts = alertRepository.findAll().stream()
                .filter(a -> a.getStatus() != AlertStatus.RESOLVED && a.getSeverity() == AlertSeverity.CRITICAL)
                .map(AlertDto::fromEntity)
                .toList();

        List<District> districts = districtRepository.findAll();
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();
        Map<Long, List<RoadSegment>> segmentsByDistrict = groupSegmentsByDistrict(allSegments);

        List<Map<String, Object>> hospitalDepotStatus = districts.stream().map(d -> {
            double score = calculateDistrictConnectivity(d, segmentsByDistrict);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("districtName", d.getName());
            map.put("state", d.getState());
            map.put("connectivityScore", Math.round(score * 10.0) / 10.0);
            map.put("accessible", score > 40.0);
            return map;
        }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("emergencyMode", emergencyMode);
        res.put("emergencyShipments", emergencyShipments);
        res.put("criticalAlerts", criticalAlerts);
        res.put("hospitalDepotAccess", hospitalDepotStatus);
        return res;
    }

    public Map<Long, List<RoadSegment>> groupSegmentsByDistrict(List<RoadSegment> segments) {
        Map<Long, List<RoadSegment>> map = new HashMap<>();
        for (RoadSegment s : segments) {
            Long fromDistId = (s.getFromNode() != null && s.getFromNode().getDistrict() != null)
                    ? s.getFromNode().getDistrict().getId() : null;
            Long toDistId = (s.getToNode() != null && s.getToNode().getDistrict() != null)
                    ? s.getToNode().getDistrict().getId() : null;

            if (fromDistId != null) {
                map.computeIfAbsent(fromDistId, k -> new ArrayList<>()).add(s);
            }
            if (toDistId != null && !toDistId.equals(fromDistId)) {
                map.computeIfAbsent(toDistId, k -> new ArrayList<>()).add(s);
            }
        }
        return map;
    }

    public double calculateDistrictConnectivity(District district) {
        List<RoadSegment> allSegments = roadSegmentRepository.findAll();
        Map<Long, List<RoadSegment>> segmentsByDistrict = groupSegmentsByDistrict(allSegments);
        return calculateDistrictConnectivity(district, segmentsByDistrict);
    }

    public double calculateDistrictConnectivity(District district, Map<Long, List<RoadSegment>> segmentsByDistrict) {
        List<RoadSegment> segments = segmentsByDistrict.getOrDefault(district.getId(), Collections.emptyList());
        return computeConnectivityScore(segments);
    }

    private double computeConnectivityScore(List<RoadSegment> segments) {
        if (segments.isEmpty()) return 100.0;

        double weightedSum = 0.0;
        double riskSum = 0.0;

        for (RoadSegment s : segments) {
            double w = s.getStatus() == SegmentStatus.OPEN ? 1.0 : s.getStatus() == SegmentStatus.RESTRICTED ? 0.5 : 0.0;
            weightedSum += w;
            riskSum += (s.getRiskScore() != null ? s.getRiskScore() : 0.0);
        }

        double ratio = (weightedSum / segments.size()) * 100.0;
        double avgRisk = riskSum / segments.size();
        double finalScore = Math.max(0.0, ratio - (avgRisk * 0.3));

        return Math.min(100.0, finalScore);
    }
}
