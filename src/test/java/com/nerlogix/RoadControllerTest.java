package com.nerlogix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.RoadStatusUpdateRequest;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.repository.AuditLogRepository;
import com.nerlogix.repository.RoadSegmentRepository;
import com.nerlogix.repository.RoadStatusHistoryRepository;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RoadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @Autowired
    private RoadStatusHistoryRepository roadStatusHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetRoads() throws Exception {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/roads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    public void testUpdateStatus_AsUser_Forbidden() throws Exception {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(user);
        RoadSegment segment = roadSegmentRepository.findAll().get(0);

        RoadStatusUpdateRequest request = new RoadStatusUpdateRequest(SegmentStatus.BLOCKED, "Landslide blocked road");

        mockMvc.perform(patch("/api/roads/" + segment.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateStatus_AsModerator_Success() throws Exception {
        User mod = userRepository.findByEmail("moderator@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(mod);
        RoadSegment segment = roadSegmentRepository.findAll().get(0);
        SegmentStatus initialStatus = segment.getStatus();
        SegmentStatus targetStatus = initialStatus == SegmentStatus.BLOCKED ? SegmentStatus.OPEN : SegmentStatus.BLOCKED;

        RoadStatusUpdateRequest request = new RoadStatusUpdateRequest(targetStatus, "Emergency clearance testing");

        mockMvc.perform(patch("/api/roads/" + segment.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(targetStatus.name()))
                .andExpect(jsonPath("$.statusReason").value("Emergency clearance testing"));

        // Verify entity updated in DB
        RoadSegment updated = roadSegmentRepository.findById(segment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(targetStatus);

        // Verify history and audit log recorded
        assertThat(roadStatusHistoryRepository.findBySegmentIdOrderByChangedAtDesc(segment.getId())).isNotEmpty();
        assertThat(auditLogRepository.findAll()).anyMatch(log -> "ROAD_STATUS_CHANGE".equals(log.getAction()));
    }
}
