package com.nerlogix.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.*;
import com.nerlogix.entity.RoadSegment;
import com.nerlogix.entity.RoadStatusHistory;
import com.nerlogix.entity.User;
import com.nerlogix.event.RoadStatusChangedEvent;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.repository.RoadSegmentRepository;
import com.nerlogix.repository.RoadStatusHistoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RoadService {

    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadStatusHistoryRepository roadStatusHistoryRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.nerlogix.service.risk.RiskEngine riskEngine;
    private final ObjectMapper objectMapper;

    public RoadService(
            RoadSegmentRepository roadSegmentRepository,
            RoadStatusHistoryRepository roadStatusHistoryRepository,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher,
            com.nerlogix.service.risk.RiskEngine riskEngine,
            ObjectMapper objectMapper) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadStatusHistoryRepository = roadStatusHistoryRepository;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.riskEngine = riskEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<RoadSegmentDto> findRoads(SegmentStatus status, String state, AssetType assetType, String riskCategory, String q, Pageable pageable) {
        Specification<RoadSegment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assetType != null) {
                predicates.add(cb.equal(root.get("assetType"), assetType));
            }
            if (StringUtils.hasText(state)) {
                predicates.add(cb.or(
                    cb.equal(root.get("fromNode").get("district").get("state"), state),
                    cb.equal(root.get("toNode").get("district").get("state"), state)
                ));
            }
            if (StringUtils.hasText(riskCategory)) {
                if ("LOW".equalsIgnoreCase(riskCategory)) {
                    predicates.add(cb.lt(root.get("riskScore"), 35.0));
                } else if ("MEDIUM".equalsIgnoreCase(riskCategory)) {
                    predicates.add(cb.and(cb.ge(root.get("riskScore"), 35.0), cb.lt(root.get("riskScore"), 60.0)));
                } else if ("HIGH".equalsIgnoreCase(riskCategory)) {
                    predicates.add(cb.and(cb.ge(root.get("riskScore"), 60.0), cb.lt(root.get("riskScore"), 80.0)));
                } else if ("CRITICAL".equalsIgnoreCase(riskCategory)) {
                    predicates.add(cb.ge(root.get("riskScore"), 80.0));
                }
            }
            if (StringUtils.hasText(q)) {
                String likePattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("highwayCode")), likePattern),
                    cb.like(cb.lower(root.get("fromNode").get("name")), likePattern),
                    cb.like(cb.lower(root.get("toNode").get("name")), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return roadSegmentRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public RoadSegmentDetailDto getRoadDetail(Long id) {
        RoadSegment segment = roadSegmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Road segment not found with id: " + id));

        List<RiskFactorDto> factors = parseRiskFactors(segment.getRiskFactors());
        List<RoadStatusHistoryDto> history = roadStatusHistoryRepository.findBySegmentIdOrderByChangedAtDesc(id)
                .stream()
                .map(h -> new RoadStatusHistoryDto(
                        h.getId(),
                        h.getSegment().getId(),
                        h.getOldStatus(),
                        h.getNewStatus(),
                        h.getReason(),
                        h.getChangedBy() != null ? h.getChangedBy().getName() : "System",
                        h.getChangedAt()
                ))
                .toList();

        String state = segment.getFromNode() != null && segment.getFromNode().getDistrict() != null
                ? segment.getFromNode().getDistrict().getState()
                : "";

        return new RoadSegmentDetailDto(
                segment.getId(),
                segment.getName(),
                segment.getHighwayCode(),
                segment.getAssetType(),
                segment.getLengthKm(),
                segment.getBaseSpeedKmph(),
                segment.getTerrainClass(),
                segment.getLandslideProne(),
                segment.getFloodProne(),
                segment.getStatus(),
                segment.getRiskScore(),
                segment.getStatusReason(),
                segment.getPolyline(),
                segment.getFromNode().getId(),
                segment.getFromNode().getName(),
                segment.getFromNode().getLat(),
                segment.getFromNode().getLng(),
                segment.getToNode().getId(),
                segment.getToNode().getName(),
                segment.getToNode().getLat(),
                segment.getToNode().getLng(),
                state,
                factors,
                history,
                segment.getUpdatedAt()
        );
    }

    @Transactional
    public RoadSegmentDto updateRoadStatus(Long id, SegmentStatus newStatus, String reason, User currentUser, String ipAddress) {
        RoadSegment segment = roadSegmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Road segment not found with id: " + id));

        SegmentStatus oldStatus = segment.getStatus();

        segment.setStatus(newStatus);
        segment.setStatusReason(reason);
        segment.setUpdatedAt(java.time.LocalDateTime.now());
        if (newStatus == SegmentStatus.BLOCKED) {
            segment.setRiskScore(100.0);
        }
        roadSegmentRepository.save(segment);

        // History entry
        RoadStatusHistory history = new RoadStatusHistory(segment, oldStatus, newStatus, reason, currentUser);
        roadStatusHistoryRepository.save(history);

        // Audit entry
        auditLogService.recordAudit(
                currentUser,
                "ROAD_STATUS_CHANGE",
                "ROAD_SEGMENT",
                segment.getId(),
                String.format("Status changed from %s to %s. Reason: %s", oldStatus, newStatus, reason),
                ipAddress
        );

        // Fire event for SSE notification
        eventPublisher.publishEvent(new RoadStatusChangedEvent(
                segment.getId(),
                segment.getName(),
                oldStatus,
                newStatus,
                reason
        ));

        riskEngine.computeSegmentRisk(segment);

        return toDto(segment);
    }

    public RoadSegmentDto toDto(RoadSegment segment) {
        String state = segment.getFromNode() != null && segment.getFromNode().getDistrict() != null
                ? segment.getFromNode().getDistrict().getState()
                : "";

        return new RoadSegmentDto(
                segment.getId(),
                segment.getName(),
                segment.getHighwayCode(),
                segment.getAssetType(),
                segment.getLengthKm(),
                segment.getBaseSpeedKmph(),
                segment.getTerrainClass(),
                segment.getLandslideProne(),
                segment.getFloodProne(),
                segment.getStatus(),
                segment.getRiskScore(),
                segment.getStatusReason(),
                segment.getPolyline(),
                segment.getFromNode() != null ? segment.getFromNode().getName() : "",
                segment.getToNode() != null ? segment.getToNode().getName() : "",
                state,
                segment.getUpdatedAt()
        );
    }

    private List<RiskFactorDto> parseRiskFactors(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<RiskFactorDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
