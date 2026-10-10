package com.nerlogix.service;

import com.nerlogix.entity.RuleConfig;
import com.nerlogix.repository.RuleConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuleConfigService {

    private final RuleConfigRepository ruleConfigRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("risk.rainfall.light", "5"),
            Map.entry("risk.rainfall.moderate", "12"),
            Map.entry("risk.rainfall.heavy", "22"),
            Map.entry("risk.rainfall.extreme", "30"),
            Map.entry("risk.rainfall.72h_bonus", "5"),
            Map.entry("risk.terrain.multiplier", "3"),
            Map.entry("risk.terrain.max", "15"),
            Map.entry("risk.terrain.landslide_bonus", "8"),
            Map.entry("risk.terrain.flood_bonus", "8"),
            Map.entry("risk.incident.severity_multiplier", "4"),
            Map.entry("risk.incident.max", "25"),
            Map.entry("risk.incident.radius_km", "15.0"),
            Map.entry("risk.status.restricted_bonus", "15"),
            Map.entry("risk.status.blocked_score", "100"),
            Map.entry("risk.congestion.bonus", "10"),
            Map.entry("risk.disruption.per_incident", "2"),
            Map.entry("risk.disruption.max", "8"),
            Map.entry("risk.disruption.days", "90"),
            Map.entry("category.low.max", "34"),
            Map.entry("category.medium.max", "59"),
            Map.entry("category.high.max", "79"),
            Map.entry("alert.high_risk.threshold", "60"),
            Map.entry("routing.risk_weight.normal", "1.5"),
            Map.entry("routing.risk_weight.emergency", "4.0"),
            Map.entry("delay.risk_multiplier", "0.8"),
            Map.entry("delay.restricted_penalty", "20"),
            Map.entry("delay.high_risk_penalty", "15")
    );

    public RuleConfigService(RuleConfigRepository ruleConfigRepository) {
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaults() {
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (ruleConfigRepository.findByConfigKey(entry.getKey()).isEmpty()) {
                ruleConfigRepository.save(new RuleConfig(entry.getKey(), entry.getValue(), "Default rule threshold for " + entry.getKey()));
            }
        }
        refreshCache();
    }

    public void refreshCache() {
        cache.clear();
        ruleConfigRepository.findAll().forEach(rc -> cache.put(rc.getConfigKey(), rc.getConfigValue()));
    }

    public String getString(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public List<RuleConfig> getAllRules() {
        return ruleConfigRepository.findAll();
    }

    @Transactional
    public void updateRules(Map<String, String> updates) {
        if (updates == null || updates.isEmpty()) return;

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();

            // Validate numeric values
            try {
                Double.parseDouble(val.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid numeric value for rule " + key + ": " + val);
            }

            RuleConfig rc = ruleConfigRepository.findByConfigKey(key)
                    .orElseGet(() -> new RuleConfig(key, val, "Custom rule setting"));
            rc.setConfigValue(val.trim());
            ruleConfigRepository.save(rc);
        }

        refreshCache();
    }
}
