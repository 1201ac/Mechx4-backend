package com.nerlogix;

import com.nerlogix.dto.DemoRainRequest;
import com.nerlogix.dto.RiskFactorDto;
import com.nerlogix.entity.Alert;
import com.nerlogix.entity.District;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.model.enums.AlertStatus;
import com.nerlogix.model.enums.AlertType;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.model.enums.WeatherCondition;
import com.nerlogix.repository.AlertRepository;
import com.nerlogix.repository.DistrictRepository;
import com.nerlogix.repository.RoadSegmentRepository;
import com.nerlogix.service.WeatherService;
import com.nerlogix.service.risk.RiskEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class RiskEngineTest {

    @Autowired
    private RiskEngine riskEngine;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private AlertRepository alertRepository;

    @Test
    @Transactional
    public void testRiskEngineFactorsClampingAndMonotonicity() {
        RoadSegment segment = roadSegmentRepository.findAll().get(0);

        // Initial compute
        RiskEngine.RiskCalculationResult result1 = riskEngine.computeSegmentRisk(segment);
        assertThat(result1.score()).isBetween(0.0, 100.0);
        assertThat(result1.factors()).isNotEmpty();

        // Monotonic rainfall test: EXTREME rain should produce higher risk than LIGHT rain
        District district = segment.getFromNode().getDistrict();
        weatherService.triggerDemoRain(district.getId(), WeatherCondition.LIGHT);
        RiskEngine.RiskCalculationResult lightResult = riskEngine.computeSegmentRisk(segment);

        weatherService.triggerDemoRain(district.getId(), WeatherCondition.EXTREME);
        RiskEngine.RiskCalculationResult extremeResult = riskEngine.computeSegmentRisk(segment);

        assertThat(extremeResult.score()).isGreaterThanOrEqualTo(lightResult.score());

        // Clamping test: score never exceeds 100
        segment.setStatus(SegmentStatus.BLOCKED);
        RiskEngine.RiskCalculationResult blockedResult = riskEngine.computeSegmentRisk(segment);
        assertThat(blockedResult.score()).isEqualTo(100.0);
    }

    @Test
    @Transactional
    public void testDemoRainRaisesRiskAndCreatesHighRiskCorridorAlert() {
        District district = districtRepository.findAll().get(0);

        // Trigger extreme rain in district
        weatherService.triggerDemoRain(district.getId(), WeatherCondition.EXTREME);

        // Check segments in district have elevated risk
        List<RoadSegment> accessSegments = roadSegmentRepository.findAll().stream()
                .filter(s -> (s.getFromNode() != null && s.getFromNode().getDistrict() != null && s.getFromNode().getDistrict().getId().equals(district.getId())) ||
                             (s.getToNode() != null && s.getToNode().getDistrict() != null && s.getToNode().getDistrict().getId().equals(district.getId())))
                .toList();

        assertThat(accessSegments).anyMatch(s -> s.getRiskScore() >= 60.0);

        // Check HIGH_RISK_CORRIDOR alert created in DB
        List<Alert> alerts = alertRepository.findAll().stream()
                .filter(a -> a.getType() == AlertType.HIGH_RISK_CORRIDOR && a.getStatus() == AlertStatus.OPEN)
                .toList();

        assertThat(alerts).isNotEmpty();
    }
}
