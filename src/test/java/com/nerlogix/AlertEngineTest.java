package com.nerlogix;

import com.nerlogix.entity.Alert;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.model.enums.AlertSeverity;

import com.nerlogix.model.enums.AlertType;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.repository.AlertRepository;
import com.nerlogix.repository.RoadSegmentRepository;
import com.nerlogix.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlertEngineTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Test
    void testAlertDeduplicationAndResolution() {
        String key = "TEST_KEY_123";

        Alert alert1 = alertService.createOrUpdateAlert(
                AlertType.ROAD_BLOCKED,
                AlertSeverity.CRITICAL,
                "ROAD_SEGMENT",
                1L,
                key,
                Map.of("en", "Test Blocked"),
                Map.of("en", "Road is blocked test")
        );

        assertThat(alert1).isNotNull();
        assertThat(alert1.getOpenKey()).isEqualTo(key);

        // Second call with same openKey should return existing alert without duplicate
        Alert alert2 = alertService.createOrUpdateAlert(
                AlertType.ROAD_BLOCKED,
                AlertSeverity.CRITICAL,
                "ROAD_SEGMENT",
                1L,
                key,
                Map.of("en", "Test Blocked"),
                Map.of("en", "Road is blocked test")
        );

        assertThat(alert2.getId()).isEqualTo(alert1.getId());

        // Resolve alert
        alertService.resolveAlertByOpenKey(key);

        Alert resolved = alertRepository.findById(alert1.getId()).orElseThrow();
        assertThat(resolved.getOpenKey()).isNull();
        assertThat(resolved.getStatus().name()).isEqualTo("RESOLVED");
    }

    @Test
    void testAutomatedAlertRules() {
        RoadSegment segment = roadSegmentRepository.findAll().stream().findFirst().orElseThrow();
        segment.setStatus(SegmentStatus.BLOCKED);
        segment.setStatusReason("Landslide blocked corridor");
        roadSegmentRepository.save(segment);

        alertService.runAlertEngine();

        List<Alert> alerts = alertRepository.findAll();
        assertThat(alerts).isNotEmpty();
        boolean hasBlockedAlert = alerts.stream()
                .anyMatch(a -> a.getType() == AlertType.ROAD_BLOCKED && a.getEntityId().equals(segment.getId()));
        assertThat(hasBlockedAlert).isTrue();
    }
}
