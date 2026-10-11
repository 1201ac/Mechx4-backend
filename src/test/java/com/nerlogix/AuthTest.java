package com.nerlogix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.LoginRequest;
import com.nerlogix.dto.RefreshTokenRequest;
import com.nerlogix.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLoginAllDemoAccounts() throws Exception {
        String[] emails = {"admin@nerlogix.com", "moderator@nerlogix.com", "user@nerlogix.com"};

        for (String email : emails) {
            LoginRequest req = new LoginRequest(email, "Demo@1234");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value(email));

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value(email));
        }
    }

    @Test
    void testRefreshTokenRotation() throws Exception {
        LoginRequest loginReq = new LoginRequest("user@nerlogix.com", "Demo@1234");
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginRes.getResponse().getContentAsString();
        String initialRefreshToken = objectMapper.readTree(body).get("refreshToken").asText();

        // 1. Refresh with initial token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshRes = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshRes.getResponse().getContentAsString()).get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(initialRefreshToken);

        // 2. Re-using initial token should fail (rotated)
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterNewUser() throws Exception {
        RegisterRequest req = new RegisterRequest("newuser@nerlogix.com", "Password123!", "New Field User", "en");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void testAuthorizationMatrixRoleCheck() throws Exception {
        // Login as USER
        LoginRequest userLogin = new LoginRequest("user@nerlogix.com", "Demo@1234");
        MvcResult userRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk()).andReturn();
        String userToken = objectMapper.readTree(userRes.getResponse().getContentAsString()).get("accessToken").asText();

        // USER accessing admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/admin/audit")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Login as ADMIN
        LoginRequest adminLogin = new LoginRequest("admin@nerlogix.com", "Demo@1234");
        MvcResult adminRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk()).andReturn();
        String adminToken = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("accessToken").asText();

        // ADMIN accessing admin endpoint -> 200 OK (or not 403)
        mockMvc.perform(get("/api/admin/audit")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testCurrentUserAndCorsPreflight() throws Exception {
        LoginRequest userLogin = new LoginRequest("user@nerlogix.com", "Demo@1234");
        MvcResult userRes = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk()).andReturn();
        String userToken = objectMapper.readTree(userRes.getResponse().getContentAsString()).get("accessToken").asText();

        // 1. GET /api/auth/me -> 200 OK
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@nerlogix.com"));

        // 2. GET /auth/me (without /api) -> 200 OK
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@nerlogix.com"));

        // 3. GET /auth/me without token -> 401 Unauthorized
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());

        // 4. OPTIONS preflight for /auth/me with origin
        mockMvc.perform(options("/auth/me")
                .header("Origin", "https://ner-logix-frontend.vercel.app")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));

        // 5. OPTIONS preflight for /api/auth/me
        mockMvc.perform(options("/api/auth/me")
                .header("Origin", "https://ner-logix-frontend.vercel.app")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
