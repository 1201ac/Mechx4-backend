package com.nerlogix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.AlertDto;
import com.nerlogix.dto.NotificationDto;
import com.nerlogix.entity.*;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final DistrictRepository districtRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuditLogService auditLogService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    public AlertService(
            AlertRepository alertRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            RoadSegmentRepository roadSegmentRepository,
            DistrictRepository districtRepository,
            ShipmentRepository shipmentRepository,
            AuditLogService auditLogService,
            SseService sseService,
            ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.districtRepository = districtRepository;
        this.shipmentRepository = shipmentRepository;
        this.auditLogService = auditLogService;
        this.sseService = sseService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Alert createOrUpdateAlert(
            AlertType type,
            AlertSeverity severity,
            String entityType,
            Long entityId,
            String openKey,
            Map<String, String> titles,
            Map<String, String> messages) {

        if (openKey != null) {
            Alert existing = alertRepository.findByOpenKey(openKey).orElse(null);
            if (existing != null) {
                return existing; // Already open or acknowledged
            }
        }

        try {
            String titleJson = objectMapper.writeValueAsString(titles);
            String messageJson = objectMapper.writeValueAsString(messages);

            Alert alert = new Alert();
            alert.setType(type);
            alert.setSeverity(severity);
            alert.setEntityType(entityType);
            alert.setEntityId(entityId);
            alert.setOpenKey(openKey);
            alert.setStatus(AlertStatus.OPEN);
            alert.setTitle(titleJson);
            alert.setMessage(messageJson);
            alert.setCreatedAt(LocalDateTime.now());

            Alert saved = alertRepository.save(alert);
            fanOutNotifications(saved, titles, messages);

            sseService.broadcast("alert.created", Map.of("alertId", saved.getId(), "type", type.name()));
            return saved;
        } catch (Exception e) {
            log.error("Failed to create alert for openKey {}", openKey, e);
            return null;
        }
    }

    @Transactional
    public void resolveAlertByOpenKey(String openKey) {
        if (openKey == null) return;
        alertRepository.findByOpenKey(openKey).ifPresent(alert -> {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setOpenKey(null); // Clear unique openKey so standard unique index allows new alerts later
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
            sseService.broadcast("alert.updated", Map.of("alertId", alert.getId(), "status", "RESOLVED"));
        });
    }

    @Transactional
    public AlertDto ackAlert(Long id, User currentUser) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert = alertRepository.save(alert);

        auditLogService.recordAudit(currentUser, "ALERT_ACKNOWLEDGED", "ALERT", alert.getId(), "Acknowledged alert #" + id, "127.0.0.1");
        sseService.broadcast("alert.updated", Map.of("alertId", alert.getId(), "status", "ACKNOWLEDGED"));

        return AlertDto.fromEntity(alert);
    }

    @Transactional
    public AlertDto resolveAlert(Long id, User currentUser) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setOpenKey(null);
        alert.setResolvedAt(LocalDateTime.now());
        alert = alertRepository.save(alert);

        auditLogService.recordAudit(currentUser, "ALERT_RESOLVED", "ALERT", alert.getId(), "Resolved alert #" + id, "127.0.0.1");
        sseService.broadcast("alert.updated", Map.of("alertId", alert.getId(), "status", "RESOLVED"));

        return AlertDto.fromEntity(alert);
    }

    public Page<AlertDto> getAlerts(AlertStatus status, AlertType type, Pageable pageable) {
        Page<Alert> page = alertRepository.findAll(pageable);
        List<AlertDto> dtos = page.getContent().stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> type == null || a.getType() == type)
                .map(AlertDto::fromEntity)
                .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // User Notifications API
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(User user, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        List<NotificationDto> dtos = page.getContent().stream().map(n -> {
            String lang = user.getLanguage() != null ? user.getLanguage() : "en";
            String parsedTitle = extractLanguageText(n.getTitle(), lang);
            String parsedMsg = extractLanguageText(n.getMessage(), lang);
            return new NotificationDto(n.getId(), n.getAlert() != null ? n.getAlert().getId() : null, parsedTitle, parsedMsg, n.getReadAt(), n.getCreatedAt());
        }).toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional
    public void markNotificationRead(Long notificationId, User user) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (n.getUser().getId().equals(user.getId())) {
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().filter(n -> n.getReadAt() == null).toList();
        for (Notification n : unread) {
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void broadcast(Map<String, String> titles, Map<String, String> messages, User sender) {
        List<User> allUsers = userRepository.findAll();
        try {
            String titleJson = objectMapper.writeValueAsString(titles);
            String messageJson = objectMapper.writeValueAsString(messages);

            for (User u : allUsers) {
                Notification n = new Notification(u, null, titleJson, messageJson);
                notificationRepository.save(n);
                sseService.sendToUser(u.getId(), "notification.created", Map.of("id", n.getId()));
            }

            auditLogService.recordAudit(sender, "NOTIFICATION_BROADCAST", "NOTIFICATION", null, "Broadcasted notification to all users", "127.0.0.1");
        } catch (Exception e) {
            log.error("Failed to broadcast notifications", e);
        }
    }

    // Automated Alert Engine (Every 30s)
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void runAlertEngine() {
        // Rule 1: BLOCKED segments -> ROAD_BLOCKED (CRITICAL)
        List<RoadSegment> blockedSegs = roadSegmentRepository.findAll().stream()
                .filter(s -> s.getStatus() == SegmentStatus.BLOCKED)
                .toList();

        for (RoadSegment seg : blockedSegs) {
            String openKey = "ROAD_BLOCKED_" + seg.getId();
            createOrUpdateAlert(
                    AlertType.ROAD_BLOCKED,
                    AlertSeverity.CRITICAL,
                    "ROAD_SEGMENT",
                    seg.getId(),
                    openKey,
                    Map.of(
                            "en", "Road Blocked: " + seg.getName(),
                            "hi", "सड़क अवरुद्ध: " + seg.getName(),
                            "as", "পথ বন্ধ: " + seg.getName(),
                            "bn", "রাস্তা অবরুদ্ধ: " + seg.getName()
                    ),
                    Map.of(
                            "en", "Segment " + seg.getName() + " is currently BLOCKED due to: " + (seg.getStatusReason() != null ? seg.getStatusReason() : "Disruption"),
                            "hi", "खंड " + seg.getName() + " वर्तमान में अवरुद्ध है।",
                            "as", "খণ্ড " + seg.getName() + " বৰ্তমান বন্ধ।",
                            "bn", "খণ্ড " + seg.getName() + " বর্তমানে অবরুদ্ধ।"
                    )
            );
        }

        // Rule 2: High risk corridors (Risk >= 60) -> HIGH_RISK_CORRIDOR (WARNING)
        List<RoadSegment> riskySegs = roadSegmentRepository.findAll().stream()
                .filter(s -> s.getStatus() != SegmentStatus.BLOCKED && s.getRiskScore() != null && s.getRiskScore() >= 60.0)
                .toList();

        for (RoadSegment seg : riskySegs) {
            String openKey = "HIGH_RISK_" + seg.getId();
            createOrUpdateAlert(
                    AlertType.HIGH_RISK_CORRIDOR,
                    AlertSeverity.WARNING,
                    "ROAD_SEGMENT",
                    seg.getId(),
                    openKey,
                    Map.of(
                            "en", "High Risk Corridor: " + seg.getName(),
                            "hi", "उच्च जोखिम वाला मार्ग: " + seg.getName(),
                            "as", "উচ্চ বিপদজনক পথ: " + seg.getName(),
                            "bn", "উচ্চ ঝুঁকির পথ: " + seg.getName()
                    ),
                    Map.of(
                            "en", "Road segment " + seg.getName() + " risk score is " + Math.round(seg.getRiskScore()) + "/100.",
                            "hi", "मार्ग " + seg.getName() + " का जोखिम स्कोर " + Math.round(seg.getRiskScore()) + "/100 है।",
                            "as", "পথ " + seg.getName() + "ৰ বিপদ স্কোৰ " + Math.round(seg.getRiskScore()) + "/100।",
                            "bn", "পথ " + seg.getName() + "-এর ঝুঁকি স্কোর " + Math.round(seg.getRiskScore()) + "/100।"
                    )
            );
        }

        // Rule 3: Shipment DELAYED -> DELIVERY_DELAYED (WARNING)
        List<Shipment> delayedShipments = shipmentRepository.findAll().stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELAYED)
                .toList();

        for (Shipment s : delayedShipments) {
            String openKey = "SHIPMENT_DELAYED_" + s.getId();
            createOrUpdateAlert(
                    AlertType.DELIVERY_DELAYED,
                    AlertSeverity.WARNING,
                    "SHIPMENT",
                    s.getId(),
                    openKey,
                    Map.of(
                            "en", "Delivery Delayed: " + s.getCode(),
                            "hi", "डिलीवरी में देरी: " + s.getCode(),
                            "as", "ডেলিভাৰীত বিলম্ব: " + s.getCode(),
                            "bn", "ডেলিভারিতে দেরি: " + s.getCode()
                    ),
                    Map.of(
                            "en", "Shipment " + s.getCode() + " (" + s.getCargoCategory() + ") is delayed by " + (s.getDelayMin() != null ? s.getDelayMin() : 30) + " mins.",
                            "hi", "शिपमेंट " + s.getCode() + " में देरी हो रही है।",
                            "as", "শিপমেন্ট " + s.getCode() + "ৰ বিলম্বর সম্মুখীন।",
                            "bn", "শিপমেন্ট " + s.getCode() + "-এ বিলম্ব হচ্ছে।"
                    )
            );
        }
    }

    private void fanOutNotifications(Alert alert, Map<String, String> titles, Map<String, String> messages) {
        List<User> targetUsers = userRepository.findAll();
        try {
            String titleJson = objectMapper.writeValueAsString(titles);
            String messageJson = objectMapper.writeValueAsString(messages);

            for (User u : targetUsers) {
                Notification n = new Notification(u, alert, titleJson, messageJson);
                notificationRepository.save(n);
                sseService.sendToUser(u.getId(), "notification.created", Map.of("id", n.getId(), "alertId", alert.getId()));
            }
        } catch (Exception e) {
            log.error("Failed to fan out notifications", e);
        }
    }

    private String extractLanguageText(String jsonText, String lang) {
        if (jsonText == null) return "";
        try {
            Map<String, String> map = objectMapper.readValue(jsonText, Map.class);
            if (map.containsKey(lang) && !map.get(lang).isBlank()) {
                return map.get(lang);
            }
            if (map.containsKey("en") && !map.get("en").isBlank()) {
                return map.get("en");
            }
            return map.values().stream().findFirst().orElse(jsonText);
        } catch (Exception e) {
            return jsonText;
        }
    }
}
