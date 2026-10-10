package com.nerlogix.service.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.RiskFactorDto;
import com.nerlogix.entity.*;
import com.nerlogix.model.enums.AlertSeverity;
import com.nerlogix.model.enums.AlertType;
import com.nerlogix.model.enums.IncidentStatus;
import com.nerlogix.model.enums.IncidentType;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.model.enums.WeatherCondition;
import com.nerlogix.repository.*;
import com.nerlogix.service.AlertService;
import com.nerlogix.service.RuleConfigService;
import com.nerlogix.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DefaultRiskEngine implements RiskEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultRiskEngine.class);

    private final RoadSegmentRepository roadSegmentRepository;
    private final WeatherLogRepository weatherLogRepository;
    private final IncidentRepository incidentRepository;
    private final DistrictRepository districtRepository;
    private final AlertService alertService;
    private final RuleConfigService ruleConfigService;
    private final ObjectMapper objectMapper;

    public DefaultRiskEngine(
            RoadSegmentRepository roadSegmentRepository,
            WeatherLogRepository weatherLogRepository,
            IncidentRepository incidentRepository,
            DistrictRepository districtRepository,
            AlertService alertService,
            RuleConfigService ruleConfigService,
            ObjectMapper objectMapper) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.weatherLogRepository = weatherLogRepository;
        this.incidentRepository = incidentRepository;
        this.districtRepository = districtRepository;
        this.alertService = alertService;
        this.ruleConfigService = ruleConfigService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RiskCalculationResult computeSegmentRisk(RoadSegment segment) {
        List<RiskFactorDto> factors = new ArrayList<>();
        double totalScore = 0.0;

        int blockedScore = ruleConfigService.getInt("risk.status.blocked_score", 100);
        int restrictedBonus = ruleConfigService.getInt("risk.status.restricted_bonus", 15);

        // Status Override Check
        if (segment.getStatus() == SegmentStatus.BLOCKED) {
            factors.add(new RiskFactorDto("Status: BLOCKED override", blockedScore));
            saveSegmentRisk(segment, (double) blockedScore, factors);
            return new RiskCalculationResult((double) blockedScore, factors);
        } else if (segment.getStatus() == SegmentStatus.RESTRICTED) {
            factors.add(new RiskFactorDto("Status: RESTRICTED override", restrictedBonus));
            totalScore += restrictedBonus;
        }

        // 1. Weather / Rainfall
        District district = segment.getFromNode() != null ? segment.getFromNode().getDistrict() : null;
        if (district == null && segment.getToNode() != null) {
            district = segment.getToNode().getDistrict();
        }

        WeatherCondition latestCond = WeatherCondition.NONE;
        Double latestRainMm = 0.0;
        List<WeatherLog> logs = List.of();

        if (district != null) {
            logs = weatherLogRepository.findByDistrictIdOrderByLoggedAtDesc(district.getId());
            if (!logs.isEmpty()) {
                WeatherLog latestLog = logs.get(0);
                latestCond = latestLog.getCondition() != null ? latestLog.getCondition() : WeatherCondition.NONE;
                latestRainMm = latestLog.getRainMm() != null ? latestLog.getRainMm() : 0.0;
            }
        }

        int pLight = ruleConfigService.getInt("risk.rainfall.light", 5);
        int pMod = ruleConfigService.getInt("risk.rainfall.moderate", 12);
        int pHeavy = ruleConfigService.getInt("risk.rainfall.heavy", 22);
        int pExtreme = ruleConfigService.getInt("risk.rainfall.extreme", 30);
        int p72hBonus = ruleConfigService.getInt("risk.rainfall.72h_bonus", 5);

        int rainPoints = 0;
        if (latestRainMm >= 100.0 || latestCond == WeatherCondition.EXTREME) {
            rainPoints = pExtreme;
            factors.add(new RiskFactorDto("Extreme rainfall / storm (>=100mm 24h)", pExtreme));
        } else if (latestCond == WeatherCondition.HEAVY) {
            rainPoints = pHeavy;
            factors.add(new RiskFactorDto("Heavy monsoon rainfall", pHeavy));
        } else if (latestCond == WeatherCondition.MODERATE) {
            rainPoints = pMod;
            factors.add(new RiskFactorDto("Moderate rainfall", pMod));
        } else if (latestCond == WeatherCondition.LIGHT) {
            rainPoints = pLight;
            factors.add(new RiskFactorDto("Light rainfall", pLight));
        }
        totalScore += rainPoints;

        // 72h total rainfall bonus: 72h total >= 150mm OR 72h condition average/max is HEAVY/EXTREME
        if (district != null && !logs.isEmpty()) {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(72);
            List<WeatherLog> logs72h = logs.stream()
                    .filter(w -> w.getLoggedAt() != null && w.getLoggedAt().isAfter(cutoff))
                    .toList();

            double total72hRain = logs72h.stream()
                    .mapToDouble(w -> w.getRainMm() != null ? w.getRainMm() : 0.0)
                    .sum();
            boolean hasHeavy72h = logs72h.stream().anyMatch(w -> w.getCondition() == WeatherCondition.HEAVY || w.getCondition() == WeatherCondition.EXTREME);

            if (total72hRain >= 150.0 || hasHeavy72h) {
                factors.add(new RiskFactorDto("High 72-hour cumulative saturation", p72hBonus));
                totalScore += p72hBonus;
            }
        }

        // 2. Terrain & Prone Factors
        int tMult = ruleConfigService.getInt("risk.terrain.multiplier", 3);
        int tMax = ruleConfigService.getInt("risk.terrain.max", 15);
        int pLandslide = ruleConfigService.getInt("risk.terrain.landslide_bonus", 8);
        int pFlood = ruleConfigService.getInt("risk.terrain.flood_bonus", 8);

        int terrainClass = segment.getTerrainClass() != null ? segment.getTerrainClass() : 1;
        int terrainPoints = Math.min(tMax, terrainClass * tMult);
        factors.add(new RiskFactorDto("Steep hill terrain class " + terrainClass, terrainPoints));
        totalScore += terrainPoints;

        if (Boolean.TRUE.equals(segment.getLandslideProne()) && latestCond != WeatherCondition.NONE) {
            factors.add(new RiskFactorDto("Landslide-prone corridor during rainfall", pLandslide));
            totalScore += pLandslide;
        }

        if (Boolean.TRUE.equals(segment.getFloodProne()) && (latestCond == WeatherCondition.MODERATE || latestCond == WeatherCondition.HEAVY || latestCond == WeatherCondition.EXTREME)) {
            factors.add(new RiskFactorDto("Flood-prone river basin during heavy rain", pFlood));
            totalScore += pFlood;
        }

        // 3. Nearby Incidents within radius
        double radiusKm = ruleConfigService.getDouble("risk.incident.radius_km", 15.0);
        int incMult = ruleConfigService.getInt("risk.incident.severity_multiplier", 4);
        int incMax = ruleConfigService.getInt("risk.incident.max", 25);
        int pCongestion = ruleConfigService.getInt("risk.congestion.bonus", 10);

        double midLat = (segment.getFromNode().getLat() + segment.getToNode().getLat()) / 2.0;
        double midLng = (segment.getFromNode().getLng() + segment.getToNode().getLng()) / 2.0;

        List<Incident> openIncidents = incidentRepository.findByStatusNot(IncidentStatus.RESOLVED);
        int incidentPoints = 0;
        boolean hasCongestionIncident = false;

        for (Incident inc : openIncidents) {
            if (inc.getStatus() == IncidentStatus.REJECTED) continue;

            double dist = GeoUtils.haversineDistanceKm(midLat, midLng, inc.getLat(), inc.getLng());
            if (dist <= radiusKm) {
                int pts = (inc.getSeverity() != null ? inc.getSeverity() : 1) * incMult;
                incidentPoints += pts;
                factors.add(new RiskFactorDto("Active verified incident nearby: " + inc.getType() + " (" + Math.round(dist) + " km)", pts));
            }
            if (inc.getSegment() != null && inc.getSegment().getId().equals(segment.getId()) && inc.getType() == IncidentType.CONGESTION) {
                hasCongestionIncident = true;
            }
        }
        incidentPoints = Math.min(incMax, incidentPoints);
        totalScore += incidentPoints;

        if (hasCongestionIncident || (segment.getStatusReason() != null && segment.getStatusReason().toLowerCase().contains("congestion"))) {
            factors.add(new RiskFactorDto("Congestion bottleneck active", pCongestion));
            totalScore += pCongestion;
        }

        // 4. Disruption history (+2 per RESOLVED incident in last 90 days, max 8)
        int pDisruption = ruleConfigService.getInt("risk.disruption.per_incident", 2);
        int pDisruptionMax = ruleConfigService.getInt("risk.disruption.max", 8);
        int disruptionDays = ruleConfigService.getInt("risk.disruption.days", 90);

        List<Incident> resolvedIncidents = incidentRepository.findBySegmentIdAndStatus(segment.getId(), IncidentStatus.RESOLVED);
        LocalDateTime historyCutoff = LocalDateTime.now().minusDays(disruptionDays);
        long recentResolvedCount = resolvedIncidents.stream()
                .filter(i -> i.getResolvedAt() != null && i.getResolvedAt().isAfter(historyCutoff))
                .count();

        if (recentResolvedCount > 0) {
            int disruptionPoints = Math.min(pDisruptionMax, (int) (recentResolvedCount * pDisruption));
            factors.add(new RiskFactorDto(String.format("Disruption history (%d resolved incidents in 90d)", recentResolvedCount), disruptionPoints));
            totalScore += disruptionPoints;
        }

        // Clamping to 0-100
        double finalScore = Math.min(100.0, Math.max(0.0, totalScore));
        saveSegmentRisk(segment, finalScore, factors);

        // Check High Risk Alert threshold
        double alertThreshold = ruleConfigService.getDouble("alert.high_risk.threshold", 60.0);
        if (finalScore >= alertThreshold && segment.getStatus() != SegmentStatus.BLOCKED) {
            String openKey = "HIGH_RISK_CORRIDOR:" + segment.getId();
            alertService.createOrUpdateAlert(
                    AlertType.HIGH_RISK_CORRIDOR,
                    AlertSeverity.WARNING,
                    "ROAD_SEGMENT",
                    segment.getId(),
                    openKey,
                    Map.of(
                            "en", "High Risk Corridor Warning",
                            "hi", "उच्च जोखिम वाला गलियारा चेतावनी",
                            "as", "উচ্চ বিপদৰ কৰিডৰ সতৰ্কবাণী",
                            "bn", "উচ্চ ঝুঁকিপূর্ণ করিডোর সতর্কতা"
                    ),
                    Map.of(
                            "en", String.format("High risk score (%d/100) detected on corridor %s (%s).", Math.round(finalScore), segment.getName(), segment.getHighwayCode()),
                            "hi", String.format("गलियारे %s पर उच्च जोखिम अंक (%d/100) दर्ज हुआ।", segment.getName(), Math.round(finalScore)),
                            "as", String.format("করিডৰ %sত উচ্চ বিপদৰ স্কোৰ (%d/100) ধৰা পৰিছে।", segment.getName(), Math.round(finalScore)),
                            "bn", String.format("করিডোর %s এ উচ্চ ঝুঁকির স্কোর (%d/100) রেকর্ড করা হয়েছে।", segment.getName(), Math.round(finalScore))
                    )
            );
        } else if (finalScore < alertThreshold) {
            alertService.resolveAlertByOpenKey("HIGH_RISK_CORRIDOR:" + segment.getId());
        }

        return new RiskCalculationResult(finalScore, factors);
    }

    @Override
    @Transactional(readOnly = true)
    public double computeDistrictConnectivity(District district) {
        List<RoadSegment> accessSegments = roadSegmentRepository.findAll().stream()
                .filter(s -> (s.getFromNode() != null && s.getFromNode().getDistrict() != null && s.getFromNode().getDistrict().getId().equals(district.getId())) ||
                             (s.getToNode() != null && s.getToNode().getDistrict() != null && s.getToNode().getDistrict().getId().equals(district.getId())))
                .toList();

        if (accessSegments.isEmpty()) return 100.0;

        double weightedUsableShare = 0.0;
        double totalRiskSum = 0.0;

        for (RoadSegment s : accessSegments) {
            if (s.getStatus() == SegmentStatus.OPEN) {
                weightedUsableShare += 1.0;
            } else if (s.getStatus() == SegmentStatus.RESTRICTED) {
                weightedUsableShare += 0.5;
            }

            totalRiskSum += (s.getRiskScore() != null ? s.getRiskScore() : 0.0);
        }

        double baseUsability = (weightedUsableShare / accessSegments.size()) * 100.0;
        double avgRisk = totalRiskSum / accessSegments.size();

        double finalScore = Math.max(0.0, baseUsability - (avgRisk * 0.3));

        boolean allBlocked = accessSegments.stream().allMatch(s -> s.getStatus() == SegmentStatus.BLOCKED);
        if (allBlocked) return 0.0;

        return finalScore;
    }

    @Override
    @Transactional
    public void recomputeAffectedSegmentsForDistrict(Long districtId) {
        List<RoadSegment> segments = roadSegmentRepository.findAll().stream()
                .filter(s -> (s.getFromNode() != null && s.getFromNode().getDistrict() != null && s.getFromNode().getDistrict().getId().equals(districtId)) ||
                             (s.getToNode() != null && s.getToNode().getDistrict() != null && s.getToNode().getDistrict().getId().equals(districtId)))
                .toList();

        for (RoadSegment s : segments) {
            computeSegmentRisk(s);
        }
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 600000) // 10 minutes scheduled recompute (C7)
    public void recomputeAll() {
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        for (RoadSegment s : segments) {
            computeSegmentRisk(s);
        }
    }

    private void saveSegmentRisk(RoadSegment segment, double score, List<RiskFactorDto> factors) {
        segment.setRiskScore(score);
        try {
            segment.setRiskFactors(objectMapper.writeValueAsString(factors));
        } catch (Exception e) {
            segment.setRiskFactors("[]");
        }
        segment.setUpdatedAt(LocalDateTime.now());
        roadSegmentRepository.save(segment);
    }
}
