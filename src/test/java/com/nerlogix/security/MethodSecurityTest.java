package com.nerlogix.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MethodSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Admin Endpoints ---

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void adminUsers_UserRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void adminUsers_ModeratorRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@nerlogix.com", roles = {"ADMIN"})
    void adminUsers_AdminRole_Success() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void adminRules_UserRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/rules"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@nerlogix.com", roles = {"ADMIN"})
    void adminRules_AdminRole_Success() throws Exception {
        mockMvc.perform(get("/api/admin/rules"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void adminApiKeys_UserRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/api-keys"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@nerlogix.com", roles = {"ADMIN"})
    void adminApiKeys_AdminRole_Success() throws Exception {
        mockMvc.perform(get("/api/admin/api-keys"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void adminAudit_UserRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@nerlogix.com", roles = {"ADMIN"})
    void adminAudit_AdminRole_Success() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk());
    }

    // --- Moderator+ Endpoints ---

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void roadStatusPatch_UserRole_Forbidden() throws Exception {
        mockMvc.perform(patch("/api/roads/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\",\"reason\":\"Test block\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void roadStatusPatch_ModeratorRole_Success() throws Exception {
        mockMvc.perform(patch("/api/roads/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\",\"reason\":\"Test block\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void vehicleCreate_UserRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regNo\":\"AS-01-AB-1234\",\"type\":\"TRUCK\",\"capacityKg\":5000,\"status\":\"IDLE\",\"lat\":26.14,\"lng\":91.73}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void vehicleCreate_ModeratorRole_Success() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regNo\":\"AS-01-AB-9999\",\"type\":\"TRUCK\",\"capacityKg\":5000,\"status\":\"IDLE\",\"lat\":26.14,\"lng\":91.73}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void broadcastNotification_UserRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titles\":{\"en\":\"Test\"},\"messages\":{\"en\":\"Body\"}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void broadcastNotification_ModeratorRole_Success() throws Exception {
        mockMvc.perform(post("/api/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titles\":{\"en\":\"Test Broadcast\"},\"messages\":{\"en\":\"Broadcasting to team\"}}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void emergencySettingsPost_UserRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/settings/emergency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void emergencySettingsPost_ModeratorRole_Success() throws Exception {
        mockMvc.perform(post("/api/settings/emergency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@nerlogix.com", roles = {"USER"})
    void updateDistrictStock_UserRole_Forbidden() throws Exception {
        mockMvc.perform(put("/api/districts/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"stockUnits\":500.0,\"dailyDemandUnits\":50.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void updateDistrictStock_ModeratorRole_Success() throws Exception {
        mockMvc.perform(put("/api/districts/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"stockUnits\":500.0,\"dailyDemandUnits\":50.0}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "moderator@nerlogix.com", roles = {"MODERATOR"})
    void adminDistricts_ModeratorRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/districts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@nerlogix.com", roles = {"ADMIN"})
    void adminDistricts_AdminRole_Success() throws Exception {
        mockMvc.perform(get("/api/admin/districts"))
                .andExpect(status().isOk());
    }
}
