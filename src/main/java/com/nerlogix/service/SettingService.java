package com.nerlogix.service;

import com.nerlogix.entity.Setting;
import com.nerlogix.repository.SettingRepository;
import com.nerlogix.service.risk.RiskEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SettingService {

    private final SettingRepository settingRepository;
    private final RiskEngine riskEngine;
    private final SseService sseService;
    private final AuditService auditService;
    private final SystemCacheService systemCacheService;

    public SettingService(
            SettingRepository settingRepository,
            RiskEngine riskEngine,
            SseService sseService,
            AuditService auditService,
            SystemCacheService systemCacheService) {
        this.settingRepository = settingRepository;
        this.riskEngine = riskEngine;
        this.sseService = sseService;
        this.auditService = auditService;
        this.systemCacheService = systemCacheService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmergencyMode() {
        boolean enabled = settingRepository.findBySettingKey("emergency_mode")
                .map(Setting::getSettingValue)
                .map(Boolean::parseBoolean)
                .orElse(false);
        return Map.of("emergencyMode", enabled);
    }

    @Transactional
    public Map<String, Object> setEmergencyMode(boolean enabled, com.nerlogix.entity.User currentUser) {
        Setting setting = settingRepository.findBySettingKey("emergency_mode")
                .orElseGet(() -> new Setting("emergency_mode", "false"));
        setting.setSettingValue(String.valueOf(enabled));
        settingRepository.save(setting);

        // Recompute risks with emergency weighting
        riskEngine.recomputeAll();

        // Broadcast SSE event
        sseService.broadcast("emergency.changed", Map.of("emergencyMode", enabled));

        // Audit log
        auditService.log(currentUser, "TOGGLE_EMERGENCY_MODE", "SETTING", setting.getId(), "Emergency mode set to " + enabled);

        // Evict cache on emergency mode change
        systemCacheService.evictEmergencyRelated();

        return Map.of("emergencyMode", enabled);
    }
}
