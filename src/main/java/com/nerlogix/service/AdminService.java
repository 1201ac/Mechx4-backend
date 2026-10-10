package com.nerlogix.service;

import com.nerlogix.dto.UserDto;
import com.nerlogix.entity.*;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.Role;
import com.nerlogix.repository.*;
import com.nerlogix.service.risk.RiskEngine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final RuleConfigService ruleConfigService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedService seedService;
    private final AuditLogService auditLogService;
    private final RiskEngine riskEngine;
    private final RoadSegmentRepository roadSegmentRepository;

    private final DistrictRepository districtRepository;

    public AdminService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            RuleConfigRepository ruleConfigRepository,
            RuleConfigService ruleConfigService,
            RefreshTokenRepository refreshTokenRepository,
            ApiKeyRepository apiKeyRepository,
            PasswordEncoder passwordEncoder,
            SeedService seedService,
            AuditLogService auditLogService,
            RiskEngine riskEngine,
            RoadSegmentRepository roadSegmentRepository,
            DistrictRepository districtRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.ruleConfigRepository = ruleConfigRepository;
        this.ruleConfigService = ruleConfigService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedService = seedService;
        this.auditLogService = auditLogService;
        this.riskEngine = riskEngine;
        this.roadSegmentRepository = roadSegmentRepository;
        this.districtRepository = districtRepository;
    }

    public Page<UserDto> getUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        List<UserDto> dtos = page.getContent().stream().map(UserDto::fromEntity).toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional
    public UserDto updateUserRole(Long userId, Role role, User currentUser) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (currentUser != null && u.getId().equals(currentUser.getId()) && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote your own admin account");
        }

        if (u.getRole() == Role.ADMIN && role != Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndActive(Role.ADMIN, true);
            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last active admin user");
            }
        }

        u.setRole(role);
        u = userRepository.save(u);
        refreshTokenRepository.deleteByUser(u);
        auditLogService.recordAudit(currentUser, "USER_ROLE_CHANGE", "USER", u.getId(), "Role updated to " + role, "127.0.0.1");
        return UserDto.fromEntity(u);
    }

    @Transactional
    public UserDto updateUserActive(Long userId, boolean active, User currentUser) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (currentUser != null && u.getId().equals(currentUser.getId()) && !active) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate your own admin account");
        }

        if (u.getRole() == Role.ADMIN && !active) {
            long activeAdminCount = userRepository.countByRoleAndActive(Role.ADMIN, true);
            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate the last active admin user");
            }
        }

        u.setActive(active);
        u = userRepository.save(u);
        refreshTokenRepository.deleteByUser(u);
        auditLogService.recordAudit(currentUser, "USER_ACTIVE_CHANGE", "USER", u.getId(), "Active status set to " + active, "127.0.0.1");
        return UserDto.fromEntity(u);
    }

    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        refreshTokenRepository.deleteByUser(u);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<RuleConfig> getRuleConfigs() {
        return ruleConfigService.getAllRules();
    }

    @Transactional
    public RuleConfig updateRuleConfig(String key, String value) {
        ruleConfigService.updateRules(Map.of(key, value));
        riskEngine.recomputeAll();
        auditLogService.recordAudit(null, "RULE_CONFIG_UPDATE", "RULE_CONFIG", null, "Updated rule " + key + " = " + value, "127.0.0.1");
        return ruleConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + key));
    }

    public List<Map<String, Object>> getApiKeys() {
        return apiKeyRepository.findAll().stream().map(k -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", k.getId());
            map.put("name", k.getName());
            map.put("active", k.getActive());
            map.put("createdAt", k.getCreatedAt());
            map.put("lastUsedAt", k.getLastUsedAt());
            return map;
        }).toList();
    }

    @Transactional
    public Map<String, String> createApiKey(String name, User currentUser) {
        String rawKey = "nk_" + UUID.randomUUID().toString().replace("-", "");
        String hash = hashKey(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setKeyHash(hash);
        apiKey.setActive(true);
        apiKey.setCreatedBy(currentUser);
        apiKey.setCreatedAt(LocalDateTime.now());

        apiKey = apiKeyRepository.save(apiKey);
        auditLogService.recordAudit(currentUser, "API_KEY_CREATED", "API_KEY", apiKey.getId(), "Created API key: " + name, "127.0.0.1");

        Map<String, String> res = new HashMap<>();
        res.put("name", name);
        res.put("apiKey", rawKey);
        return res;
    }

    @Transactional
    public void deleteApiKey(Long id) {
        apiKeyRepository.deleteById(id);
        auditLogService.recordAudit(null, "API_KEY_DELETED", "API_KEY", id, "Deleted API key #" + id, "127.0.0.1");
    }

    @Transactional
    public void resetDemoData() {
        try {
            seedService.run();
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset demo data", e);
        }
    }

    @Transactional
    public com.nerlogix.dto.RoadSegmentDto snapRoadGeometry(Long segmentId) {
        RoadSegment seg = roadSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Road segment not found: " + segmentId));

        RoadNode from = seg.getFromNode();
        RoadNode to = seg.getToNode();

        List<double[]> points = new ArrayList<>();
        int numPoints = 6;
        Random rand = new Random(segmentId);
        for (int i = 0; i <= numPoints; i++) {
            double ratio = (double) i / numPoints;
            double lat = from.getLat() + (to.getLat() - from.getLat()) * ratio + (rand.nextDouble() - 0.5) * 0.008;
            double lng = from.getLng() + (to.getLng() - from.getLng()) * ratio + (rand.nextDouble() - 0.5) * 0.008;
            points.add(new double[]{lat, lng});
        }
        try {
            seg.setPolyline(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(points));
        } catch (Exception e) {
            seg.setPolyline("[]");
        }
        seg.setUpdatedAt(LocalDateTime.now());
        seg = roadSegmentRepository.save(seg);
        String state = from.getDistrict() != null ? from.getDistrict().getState() : "Assam";
        return new com.nerlogix.dto.RoadSegmentDto(
            seg.getId(),
            seg.getName(),
            seg.getHighwayCode(),
            seg.getAssetType(),
            seg.getLengthKm(),
            seg.getBaseSpeedKmph(),
            seg.getTerrainClass(),
            seg.getLandslideProne(),
            seg.getFloodProne(),
            seg.getStatus(),
            seg.getRiskScore(),
            seg.getStatusReason(),
            seg.getPolyline(),
            from.getName(),
            to.getName(),
            state,
            seg.getUpdatedAt()
        );
    }

    public List<District> getAllDistricts() {
        return districtRepository.findAll();
    }

    @Transactional
    public District updateDistrict(Long id, com.nerlogix.dto.UpdateDistrictRequest req, User currentUser) {
        District d = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + id));
        d.setName(req.name());
        d.setState(req.state());
        d.setLat(req.lat());
        d.setLng(req.lng());
        d.setPopulationBand(req.populationBand());
        d.setUpdatedAt(LocalDateTime.now());
        d = districtRepository.save(d);
        auditLogService.recordAudit(currentUser, "UPDATE_DISTRICT", "DISTRICT", d.getId(), "Updated district fields for " + d.getName(), "127.0.0.1");
        return d;
    }

    public List<RoadSegment> getAllRoads() {
        return roadSegmentRepository.findAll();
    }

    @Transactional
    public RoadSegment updateRoadSegment(Long id, com.nerlogix.dto.UpdateRoadSegmentRequest req, User currentUser) {
        RoadSegment seg = roadSegmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Road segment not found: " + id));
        seg.setName(req.name());
        seg.setAssetType(req.assetType());
        seg.setBaseSpeedKmph(req.baseSpeedKmph());
        seg.setTerrainClass(req.terrainClass());
        seg.setLandslideProne(req.landslideProne());
        seg.setFloodProne(req.floodProne());
        seg.setUpdatedAt(LocalDateTime.now());
        seg = roadSegmentRepository.save(seg);

        riskEngine.recomputeAll();
        auditLogService.recordAudit(currentUser, "UPDATE_ROAD_SEGMENT", "ROAD_SEGMENT", seg.getId(), "Updated road segment fields for " + seg.getName(), "127.0.0.1");
        return seg;
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
