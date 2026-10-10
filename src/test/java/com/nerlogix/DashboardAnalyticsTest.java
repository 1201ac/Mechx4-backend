package com.nerlogix;

import com.nerlogix.service.DashboardService;
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
public class DashboardAnalyticsTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void testDashboardSummaryAndAnalytics() {
        Map<String, Object> summary = dashboardService.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary).containsKey("totalDistricts");
        assertThat(summary).containsKey("activeVehicles");
        assertThat(summary).containsKey("supplyGaps");

        List<Map<String, Object>> districts = dashboardService.getDistrictConnectivityList();
        assertThat(districts).isNotEmpty();

        List<Map<String, Object>> supply = dashboardService.getSupplyStatus();
        assertThat(supply).isNotEmpty();

        Map<String, Object> charts = dashboardService.getAnalyticsCharts();
        assertThat(charts).containsKey("roadStatusBreakdown");
        assertThat(charts).containsKey("incidentsByType");
    }
}
