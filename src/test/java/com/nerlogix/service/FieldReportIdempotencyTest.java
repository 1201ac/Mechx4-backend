package com.nerlogix.service;

import com.nerlogix.dto.FieldReportPayload;

import com.nerlogix.entity.User;
import com.nerlogix.model.enums.IncidentType;
import com.nerlogix.model.enums.Role;
import com.nerlogix.repository.FieldReportRepository;

import com.nerlogix.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FieldReportIdempotencyTest {

    @Autowired
    private FieldReportService fieldReportService;

    @Autowired
    private FieldReportRepository fieldReportRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("field_user@nerlogix.com", "pass", "Field User", Role.USER));
    }

    @Test
    void testSameClientIdDuplicateCheck() {
        String clientId = "CLIENT-TEST-UUID-999";
        FieldReportPayload payload = new FieldReportPayload(
                clientId, 26.14, 91.73, 10.0, LocalDateTime.now(), IncidentType.LANDSLIDE, 3, "Big rock on road"
        );

        var res1 = fieldReportService.createFieldReport(payload, null, user);
        assertThat(res1.get("duplicate")).isEqualTo(false);

        long countAfterFirst = fieldReportRepository.count();

        // Submit again with same clientId
        var res2 = fieldReportService.createFieldReport(payload, null, user);
        assertThat(res2.get("duplicate")).isEqualTo(true);

        long countAfterSecond = fieldReportRepository.count();
        assertThat(countAfterSecond).isEqualTo(countAfterFirst);
    }
}
