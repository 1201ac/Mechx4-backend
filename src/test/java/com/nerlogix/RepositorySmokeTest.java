package com.nerlogix;

import com.nerlogix.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RepositorySmokeTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private DistrictRepository districtRepository;
    @Autowired private RoadNodeRepository roadNodeRepository;
    @Autowired private RoadSegmentRepository roadSegmentRepository;
    @Autowired private RoadStatusHistoryRepository roadStatusHistoryRepository;
    @Autowired private WeatherLogRepository weatherLogRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private VehiclePositionRepository vehiclePositionRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private RoutePlanRepository routePlanRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository incidentEventRepository;
    @Autowired private FieldReportRepository fieldReportRepository;
    @Autowired private ReportImageRepository reportImageRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private DistrictStockRepository districtStockRepository;
    @Autowired private RuleConfigRepository ruleConfigRepository;
    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private SettingRepository settingRepository;

    @Test
    void allRepositoriesCountExecution() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(refreshTokenRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(districtRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(roadNodeRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(roadSegmentRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(roadStatusHistoryRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(weatherLogRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(vehicleRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(vehiclePositionRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(shipmentRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(routePlanRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(incidentRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(incidentEventRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(fieldReportRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(reportImageRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(alertRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(notificationRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(districtStockRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(ruleConfigRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(apiKeyRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(auditLogRepository.count()).isGreaterThanOrEqualTo(0);
        assertThat(settingRepository.count()).isGreaterThanOrEqualTo(0);
    }
}
