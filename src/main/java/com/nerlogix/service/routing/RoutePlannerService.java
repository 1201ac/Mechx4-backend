package com.nerlogix.service.routing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.RouteOptionDto;
import com.nerlogix.dto.RoutePlanRequest;
import com.nerlogix.entity.RoadNode;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.entity.Setting;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.repository.RoadNodeRepository;
import com.nerlogix.repository.RoadSegmentRepository;
import com.nerlogix.repository.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RoutePlannerService {

    private static final Logger log = LoggerFactory.getLogger(RoutePlannerService.class);

    private final RoadNodeRepository roadNodeRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper;
    private final RoadGraphCache roadGraphCache;

    public RoutePlannerService(
            RoadNodeRepository roadNodeRepository,
            RoadSegmentRepository roadSegmentRepository,
            SettingRepository settingRepository,
            ObjectMapper objectMapper,
            RoadGraphCache roadGraphCache) {
        this.roadNodeRepository = roadNodeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.settingRepository = settingRepository;
        this.objectMapper = objectMapper;
        this.roadGraphCache = roadGraphCache;
    }

    @Transactional(readOnly = true)
    public List<RouteOptionDto> planRoutes(RoutePlanRequest req) {
        RoadNode origin = roadNodeRepository.findById(req.originNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Origin node not found with id: " + req.originNodeId()));
        RoadNode destination = roadNodeRepository.findById(req.destinationNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination node not found with id: " + req.destinationNodeId()));

        boolean emergencyMode = settingRepository.findBySettingKey("emergency_mode")
                .map(Setting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(false);

        List<RoadSegment> allSegments = roadGraphCache.getSegments();
        List<RoadSegment> usableSegments = allSegments.stream()
                .filter(s -> emergencyMode ? s.getStatus() == SegmentStatus.OPEN : s.getStatus() != SegmentStatus.BLOCKED)
                .toList();

        Set<Long> penalizedEdgeIds = new HashSet<>();
        List<List<RoadSegment>> foundPaths = new ArrayList<>();

        for (int attempt = 0; attempt < 3; attempt++) {
            List<RoadSegment> path = runDijkstra(origin.getId(), destination.getId(), usableSegments, penalizedEdgeIds, emergencyMode);
            if (path == null || path.isEmpty()) break;

            // Check if path is duplicate
            boolean isDuplicate = foundPaths.stream().anyMatch(existing -> getSegmentIds(existing).equals(getSegmentIds(path)));
            if (!isDuplicate) {
                foundPaths.add(path);
            }

            // Penalize all edges in found path for next iterations
            path.forEach(s -> penalizedEdgeIds.add(s.getId()));
        }

        if (foundPaths.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No navigable route found from %s to %s under current road status.", origin.getName(), destination.getName()));
        }

        List<RouteOptionDto> routeOptions = new ArrayList<>();
        for (int i = 0; i < foundPaths.size(); i++) {
            List<RoadSegment> path = foundPaths.get(i);
            routeOptions.add(buildRouteOptionDto(i + 1, path, origin, destination, emergencyMode));
        }

        return routeOptions;
    }

    private List<RoadSegment> runDijkstra(
            Long originId,
            Long destId,
            List<RoadSegment> availableSegments,
            Set<Long> penalizedEdgeIds,
            boolean emergencyMode) {

        Map<Long, List<RoadSegment>> nodeAdjacency = new HashMap<>();
        for (RoadSegment s : availableSegments) {
            nodeAdjacency.computeIfAbsent(s.getFromNode().getId(), k -> new ArrayList<>()).add(s);
            nodeAdjacency.computeIfAbsent(s.getToNode().getId(), k -> new ArrayList<>()).add(s);
        }

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, RoadSegment> prevEdge = new HashMap<>();
        Map<Long, Long> prevNode = new HashMap<>();
        PriorityQueue<Long> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        dist.put(originId, 0.0);
        pq.add(originId);

        while (!pq.isEmpty()) {
            Long current = pq.poll();
            if (current.equals(destId)) break;

            List<RoadSegment> edges = nodeAdjacency.getOrDefault(current, Collections.emptyList());
            for (RoadSegment edge : edges) {
                Long neighbor = edge.getFromNode().getId().equals(current) ? edge.getToNode().getId() : edge.getFromNode().getId();

                double length = edge.getLengthKm() != null ? edge.getLengthKm() : 10.0;
                double speed = edge.getBaseSpeedKmph() != null ? edge.getBaseSpeedKmph() : 40.0;
                double baseTimeMin = (length / speed) * 60.0;

                double riskScore = edge.getRiskScore() != null ? edge.getRiskScore() : 0.0;
                double riskWeight = emergencyMode ? (1.0 + (riskScore / 100.0) * 4.0) : (1.0 + (riskScore / 100.0) * 1.5);
                double penaltyMult = penalizedEdgeIds.contains(edge.getId()) ? 3.0 : 1.0;

                double edgeCost = baseTimeMin * riskWeight * penaltyMult;
                double newDist = dist.getOrDefault(current, Double.MAX_VALUE) + edgeCost;

                if (newDist < dist.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    dist.put(neighbor, newDist);
                    prevEdge.put(neighbor, edge);
                    prevNode.put(neighbor, current);
                    pq.add(neighbor);
                }
            }
        }

        if (!dist.containsKey(destId) || dist.get(destId) == Double.MAX_VALUE) {
            return Collections.emptyList();
        }

        // Reconstruct path
        List<RoadSegment> path = new ArrayList<>();
        Long curr = destId;
        while (!curr.equals(originId)) {
            RoadSegment edge = prevEdge.get(curr);
            if (edge == null) break;
            path.add(0, edge);
            curr = prevNode.get(curr);
        }

        return path;
    }

    private RouteOptionDto buildRouteOptionDto(
            int optionIndex,
            List<RoadSegment> path,
            RoadNode origin,
            RoadNode destination,
            boolean emergencyMode) {

        double totalDistance = 0.0;
        double totalBaseTimeMin = 0.0;
        double totalRiskWeightSum = 0.0;
        boolean hasRestricted = false;
        boolean hasHighRisk = false;

        List<Long> segmentIds = new ArrayList<>();
        List<String> segmentNames = new ArrayList<>();
        List<List<Double>> polylinePoints = new ArrayList<>();

        for (RoadSegment s : path) {
            segmentIds.add(s.getId());
            segmentNames.add(s.getName());

            double length = s.getLengthKm() != null ? s.getLengthKm() : 10.0;
            double speed = s.getBaseSpeedKmph() != null ? s.getBaseSpeedKmph() : 40.0;
            double baseTime = (length / speed) * 60.0;

            totalDistance += length;
            totalBaseTimeMin += baseTime;

            double riskScore = s.getRiskScore() != null ? s.getRiskScore() : 0.0;
            totalRiskWeightSum += riskScore * length;

            if (s.getStatus() == SegmentStatus.RESTRICTED) hasRestricted = true;
            if (riskScore >= 60.0) hasHighRisk = true;

            // Merge geometry points
            if (s.getPolyline() != null) {
                try {
                    List<List<Double>> pts = objectMapper.readValue(s.getPolyline(), new TypeReference<List<List<Double>>>() {});
                    polylinePoints.addAll(pts);
                } catch (Exception e) {
                    // Fallback to straight line points between nodes
                    polylinePoints.add(List.of(s.getFromNode().getLat(), s.getFromNode().getLng()));
                    polylinePoints.add(List.of(s.getToNode().getLat(), s.getToNode().getLng()));
                }
            } else {
                polylinePoints.add(List.of(s.getFromNode().getLat(), s.getFromNode().getLng()));
                polylinePoints.add(List.of(s.getToNode().getLat(), s.getToNode().getLng()));
            }
        }

        double weightedAvgRisk = totalDistance > 0 ? totalRiskWeightSum / totalDistance : 0.0;
        String riskCategory = weightedAvgRisk >= 80.0 ? "CRITICAL" : weightedAvgRisk >= 60.0 ? "HIGH" : weightedAvgRisk >= 35.0 ? "MEDIUM" : "LOW";

        // Delay estimate formula
        double delayRiskMin = totalBaseTimeMin * (weightedAvgRisk / 100.0 * 0.8);
        int expectedDelayMin = (int) Math.round(delayRiskMin + (hasRestricted ? 20 : 0) + (hasHighRisk ? 15 : 0));
        int totalEtaMin = (int) Math.round(totalBaseTimeMin + expectedDelayMin);

        String optionName = optionIndex == 1 ? "Primary Recommended Route" : "Alternative Option " + optionIndex;
        String reason;
        if (optionIndex == 1) {
            reason = String.format("Fastest available route from %s to %s (%d km, ~%d min).", origin.getName(), destination.getName(), Math.round(totalDistance), totalEtaMin);
        } else {
            reason = String.format("Alternative corridor bypassing high risk segments (+%d min extra delay).", expectedDelayMin);
        }

        String geometryJson;
        try {
            geometryJson = objectMapper.writeValueAsString(polylinePoints);
        } catch (Exception e) {
            geometryJson = "[]";
        }

        return new RouteOptionDto(
                optionIndex,
                optionName,
                Math.round(totalDistance * 10.0) / 10.0,
                totalEtaMin,
                Math.round(weightedAvgRisk * 10.0) / 10.0,
                riskCategory,
                expectedDelayMin,
                reason,
                geometryJson,
                segmentIds,
                segmentNames,
                emergencyMode
        );
    }

    private List<Long> getSegmentIds(List<RoadSegment> segments) {
        return segments.stream().map(RoadSegment::getId).toList();
    }
}
