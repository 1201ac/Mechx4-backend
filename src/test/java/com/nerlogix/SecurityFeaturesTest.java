package com.nerlogix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.LoginRequest;
import com.nerlogix.dto.RefreshTokenRequest;
import com.nerlogix.dto.RegisterRequest;
import com.nerlogix.dto.TokenRefreshResponse;
import com.nerlogix.entity.User;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.security.JwtTokenProvider;
import com.nerlogix.service.AdminService;
import com.nerlogix.service.AuthService;
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
public class SecurityFeaturesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void testPasswordPolicyValidation() throws Exception {
        // Weak password (no uppercase/digit/special) -> 400
        RegisterRequest weakReq = new RegisterRequest("weakpass@nerlogix.com", "weakpassword", "Weak User", "en");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character."));

        // Strong password -> 200 OK
        RegisterRequest strongReq = new RegisterRequest("strongpass@nerlogix.com", "StrongP@ss123", "Strong User", "en");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(strongReq)))
                .andExpect(status().isOk());
    }

    @Test
    void testApiKeyAuthentication() throws Exception {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();
        Map<String, String> keyMap = adminService.createApiKey("Integration Key", admin);
        String rawKey = keyMap.get("apiKey");

        // Missing key -> 401
        mockMvc.perform(post("/api/integrations/road-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segmentId\":1,\"status\":\"BLOCKED\"}"))
                .andExpect(status().isUnauthorized());

        // Invalid key -> 401
        mockMvc.perform(post("/api/integrations/road-status")
                        .header("X-API-Key", "invalid_key_12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segmentId\":1,\"status\":\"BLOCKED\"}"))
                .andExpect(status().isUnauthorized());

        // Valid key -> 200 OK
        mockMvc.perform(post("/api/integrations/road-status")
                        .header("X-API-Key", rawKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"segmentId\":1,\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryParamTokenDisabledForSse() throws Exception {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        String token = tokenProvider.generateAccessToken(user);

        // Query parameter token -> 401 Unauthorized
        mockMvc.perform(get("/api/realtime/stream").param("token", token))
                .andExpect(status().isUnauthorized());

        // Header token -> 200 OK
        mockMvc.perform(get("/api/realtime/stream").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/realtime/stream").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginRateLimiter() throws Exception {
        LoginRequest badLogin = new LoginRequest("user@nerlogix.com", "WrongPassword123!");
        String testIp = "192.168.1.200";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt from same IP -> 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testRefreshTokenReuseDetection() {
        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();

        // Perform initial refresh token creation via register
        RegisterRequest req = new RegisterRequest("refreshtest@nerlogix.com", "DemoP@ss123!", "Refresh Test", "en");
        var authRes = authService.register(req);
        String initialRefreshToken = authRes.refreshToken();

        // 1st use: rotate to new refresh token
        TokenRefreshResponse firstRotate = authService.refreshToken(new RefreshTokenRequest(initialRefreshToken));
        assertThat(firstRotate.refreshToken()).isNotNull().isNotEqualTo(initialRefreshToken);

        // Reuse initial refresh token (already revoked) -> trigger family revocation & return 401 (BadCredentialsException)
        try {
            authService.refreshToken(new RefreshTokenRequest(initialRefreshToken));
            org.junit.jupiter.api.Assertions.fail("Expected BadCredentialsException on refresh token reuse");
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            assertThat(ex.getMessage()).contains("reuse detected");
        }

        // Subsequent use of the rotated token also fails because family was revoked
        try {
            authService.refreshToken(new RefreshTokenRequest(firstRotate.refreshToken()));
            org.junit.jupiter.api.Assertions.fail("Expected BadCredentialsException after family revocation");
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            assertThat(ex.getMessage()).contains("Invalid refresh token");
        }
    }

    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    @Test
    void testDeactivatedUserJwtInvalidation() throws Exception {
        User tempUser = userRepository.save(new User("deact@nerlogix.com", "hash", "Deact User", com.nerlogix.model.enums.Role.USER));
        String token = tokenProvider.generateAccessToken(tempUser);

        // Active user -> 200 OK
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Deactivate user
        tempUser.setActive(false);
        userRepository.save(tempUser);

        // Deactivated user -> 401 Unauthorized
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
