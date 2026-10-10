package com.nerlogix;

import com.nerlogix.dto.RouteOptionDto;
import com.nerlogix.dto.RoutePlanRequest;
import com.nerlogix.entity.RoadNode;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.entity.Setting;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.repository.RoadNodeRepository;
import com.nerlogix.repository.RoadSegmentRepository;
import com.nerlogix.repository.SettingRepository;
import com.nerlogix.service.routing.RoutePlannerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class RoutePlannerServiceTest {

    @Autowired
    private RoutePlannerService routePlannerService;

    @Autowired
    private RoadNodeRepository roadNodeRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private SettingRepository settingRepository;

    @Test
    @Transactional
    public void testPlanRoutesGuwahatiToImphal() {
        RoadNode guwahati = roadNodeRepository.findByName("Guwahati").orElseThrow();
        RoadNode imphal = roadNodeRepository.findByName("Imphal").orElseThrow();

        RoutePlanRequest req = new RoutePlanRequest(guwahati.getId(), imphal.getId(), null, null);
        List<RouteOptionDto> options = routePlannerService.planRoutes(req);

        assertThat(options).isNotEmpty();
        assertThat(options.get(0).distanceKm()).isGreaterThan(0);
        assertThat(options.get(0).etaMinutes()).isGreaterThan(0);
        assertThat(options.get(0).segmentIds()).isNotEmpty();
    }

    @Test
    @Transactional
    public void testBlockedSegmentForcesAlternativeRoute() {
        RoadNode guwahati = roadNodeRepository.findByName("Guwahati").orElseThrow();
        RoadNode imphal = roadNodeRepository.findByName("Imphal").orElseThrow();

        // Plan initial route
        RoutePlanRequest req = new RoutePlanRequest(guwahati.getId(), imphal.getId(), null, null);
        List<RouteOptionDto> initialOptions = routePlannerService.planRoutes(req);
        Long primarySegmentId = initialOptions.get(0).segmentIds().get(0);

        // Block the first segment of primary route
        RoadSegment primarySegment = roadSegmentRepository.findById(primarySegmentId).orElseThrow();
        primarySegment.setStatus(SegmentStatus.BLOCKED);
        roadSegmentRepository.save(primarySegment);

        // Plan route again
        List<RouteOptionDto> newOptions = routePlannerService.planRoutes(req);

        assertThat(newOptions).isNotEmpty();
        assertThat(newOptions.get(0).segmentIds()).doesNotContain(primarySegmentId);
    }

    @Test
    @Transactional
    public void testEmergencyModeExcludesRestrictedSegments() {
        RoadNode origin = roadNodeRepository.findByName("Guwahati").orElseThrow();
        RoadNode dest = roadNodeRepository.findByName("Nagaon").orElseThrow();

        // Mark a segment on Guwahati-Nagaon corridor as RESTRICTED
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        RoadSegment restrictedSegment = segments.stream()
                .filter(s -> (s.getFromNode().getName().equals("Guwahati") || s.getToNode().getName().equals("Guwahati")))
                .findFirst().orElseThrow();

        restrictedSegment.setStatus(SegmentStatus.RESTRICTED);
        roadSegmentRepository.save(restrictedSegment);

        // Enable emergency mode in settings
        Setting setting = settingRepository.findBySettingKey("emergency_mode").orElseGet(() -> new Setting("emergency_mode", "true"));
        setting.setSettingValue("true");
        settingRepository.save(setting);

        // Plan route
        RoutePlanRequest req = new RoutePlanRequest(origin.getId(), dest.getId(), null, null);
        List<RouteOptionDto> options = routePlannerService.planRoutes(req);

        assertThat(options).isNotEmpty();
        assertThat(options.get(0).isEmergencyOptimized()).isTrue();
        assertThat(options.get(0).segmentIds()).doesNotContain(restrictedSegment.getId());
    }
}
