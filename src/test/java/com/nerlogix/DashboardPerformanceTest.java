package com.nerlogix;

import com.nerlogix.service.DashboardService;
import com.nerlogix.service.SystemCacheService;
import org.junit.jupiter.api.BeforeEach;
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
public class DashboardPerformanceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SystemCacheService systemCacheService;

    @BeforeEach
    void setUp() {
        systemCacheService.evictAll();
    }

    @Test
    void testDistrictConnectivityListIsFastAndCached() {
        // First call: populates cache with single pass over segments
        long start1 = System.currentTimeMillis();
        List<Map<String, Object>> result1 = dashboardService.getDistrictConnectivityList();
        long duration1 = System.currentTimeMillis() - start1;

        assertThat(result1).isNotEmpty();
        assertThat(duration1).isLessThan(2000); // Well under 2s even cold

        // Second call: warm cache returns in under 50ms without re-querying
        long start2 = System.currentTimeMillis();
        List<Map<String, Object>> result2 = dashboardService.getDistrictConnectivityList();
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(result2).isEqualTo(result1);
        assertThat(duration2).isLessThan(100); // Sub-100ms warm response
    }

    @Test
    void testDashboardSummaryIsFastAndCached() {
        long start1 = System.currentTimeMillis();
        Map<String, Object> summary1 = dashboardService.getSummary();
        long duration1 = System.currentTimeMillis() - start1;

        assertThat(summary1).containsKey("totalDistricts");
        assertThat(duration1).isLessThan(2000);

        long start2 = System.currentTimeMillis();
        Map<String, Object> summary2 = dashboardService.getSummary();
        long duration2 = System.currentTimeMillis() - start2;

        assertThat(summary2).isEqualTo(summary1);
        assertThat(duration2).isLessThan(100);
    }

    @Test
    void testCacheEvictionOnChanges() {
        dashboardService.getSummary();
        dashboardService.getDistrictConnectivityList();

        // Evict caches
        systemCacheService.evictRoadRelated();

        // Fresh computation after eviction
        Map<String, Object> summaryAfterEvict = dashboardService.getSummary();
        assertThat(summaryAfterEvict).isNotNull();
    }
}
