package com.nerlogix;

import com.nerlogix.entity.User;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminIntegrationsTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testApiKeyGenerationAndRuleConfig() {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();

        Map<String, String> key = adminService.createApiKey("Test System Key", admin);
        assertThat(key).containsKey("apiKey");
        assertThat(key.get("apiKey")).startsWith("nk_");

        assertThat(adminService.getApiKeys()).isNotEmpty();

        adminService.updateRuleConfig("test_threshold", "85");
        boolean hasConfig = adminService.getRuleConfigs().stream()
                .anyMatch(c -> c.getConfigKey().equals("test_threshold") && c.getConfigValue().equals("85"));
        assertThat(hasConfig).isTrue();
    }
}
