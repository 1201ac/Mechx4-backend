package com.nerlogix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.BroadcastNotificationRequest;
import com.nerlogix.entity.User;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MissingFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void testSearchEndpoint() throws Exception {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/search").param("q", "Guwahati").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.segments").isArray());
    }

    @Test
    void testWeatherLogsListing() throws Exception {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/weather/logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testMovementAndEmergencyDashboards() throws Exception {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/dashboard/movement").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments").isArray());

        mockMvc.perform(get("/api/dashboard/emergency").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emergencyMode").exists());
    }

    @Test
    void testEmergencyModeToggleSettings() throws Exception {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(admin);

        mockMvc.perform(get("/api/settings/emergency").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/settings/emergency")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emergencyMode").value(true));
    }

    @Test
    void testNotificationBroadcast() throws Exception {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(admin);

        BroadcastNotificationRequest req = new BroadcastNotificationRequest(
                Map.of("en", "Alert Title"),
                Map.of("en", "Alert Message Content")
        );

        mockMvc.perform(post("/api/notifications/broadcast")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLogAndRoadSnapGeometry() throws Exception {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(admin);

        // Audit Log
        mockMvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Snap geometry
        mockMvc.perform(post("/api/admin/roads/snap-geometry")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segmentId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.polyline").isNotEmpty());
    }
}
