package com.nerlogix.service;

import com.nerlogix.dto.*;
import com.nerlogix.entity.*;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;
import com.nerlogix.service.risk.RiskEngine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadStatusHistoryRepository roadStatusHistoryRepository;
    private final DistrictRepository districtRepository;
    private final AuditLogService auditLogService;
    private final RiskEngine riskEngine;
    private final SseService sseService;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            RoadSegmentRepository roadSegmentRepository,
            RoadStatusHistoryRepository roadStatusHistoryRepository,
            DistrictRepository districtRepository,
            AuditLogService auditLogService,
            RiskEngine riskEngine,
            SseService sseService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadStatusHistoryRepository = roadStatusHistoryRepository;
        this.districtRepository = districtRepository;
        this.auditLogService = auditLogService;
        this.riskEngine = riskEngine;
        this.sseService = sseService;
    }

    public Page<IncidentDto> getIncidents(
            IncidentStatus status,
            IncidentType type,
            Long districtId,
            Long segmentId,
            String q,
            Pageable pageable) {

        final String fQ = (q != null && !q.isBlank()) ? q.toLowerCase() : null;

        Page<Incident> page = incidentRepository.findAll(pageable);

        List<IncidentDto> filtered = page.getContent().stream()
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> type == null || i.getType() == type)
                .filter(i -> districtId == null || (i.getDistrict() != null && i.getDistrict().getId().equals(districtId)))
                .filter(i -> segmentId == null || (i.getSegment() != null && i.getSegment().getId().equals(segmentId)))
                .filter(i -> fQ == null || (i.getDescription() != null && i.getDescription().toLowerCase().contains(fQ)))
                .map(IncidentDto::fromEntity)
                .toList();

        return new PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    public IncidentDto getIncidentById(Long id) {
        Incident inc = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
        return IncidentDto.fromEntity(inc);
    }

    @Transactional
    public IncidentDto createIncident(CreateIncidentRequest req, User currentUser) {
        RoadSegment seg = req.segmentId() != null ? roadSegmentRepository.findById(req.segmentId()).orElse(null) : null;
        District dist = req.districtId() != null ? districtRepository.findById(req.districtId()).orElse(null) : null;

        Incident inc = new Incident();
        inc.setType(req.type());
        inc.setSeverity(req.severity());
        inc.setLat(req.lat());
        inc.setLng(req.lng());
        inc.setSegment(seg);
        inc.setDistrict(dist);
        inc.setDescription(req.description());
        inc.setStatus(IncidentStatus.VERIFIED);
        inc.setSource(currentUser.getRole() == Role.USER ? IncidentSource.FIELD : IncidentSource.SYSTEM);
        inc.setCreatedBy(currentUser);

        inc = incidentRepository.save(inc);

        incidentEventRepository.save(new IncidentEvent(inc, "CREATED", currentUser, "Incident logged directly"));

        if (seg != null) {
            SegmentStatus oldStatus = seg.getStatus();
            if (req.severity() == 3 && oldStatus != SegmentStatus.BLOCKED) {
                seg.setStatus(SegmentStatus.RESTRICTED);
            } else if (req.severity() == 4) {
                seg.setStatus(SegmentStatus.BLOCKED);
            }
            if (seg.getStatus() != oldStatus) {
                seg.setStatusReason("Incident logged: " + req.type() + " severity " + req.severity());
                roadSegmentRepository.save(seg);
                roadStatusHistoryRepository.save(new RoadStatusHistory(seg, oldStatus, seg.getStatus(), seg.getStatusReason(), currentUser));
                sseService.broadcast("road.updated", Map.of("segmentId", seg.getId(), "status", seg.getStatus()));
            }
            riskEngine.computeSegmentRisk(seg);
        }

        auditLogService.recordAudit(currentUser, "INCIDENT_CREATED", "INCIDENT", inc.getId(), "Created incident " + inc.getType(), "127.0.0.1");
        return IncidentDto.fromEntity(inc);
    }

    @Transactional
    public IncidentDto resolveIncident(Long id, String note, User moderator) {
        Incident inc = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));

        if (inc.getStatus() == IncidentStatus.RESOLVED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Incident #" + id + " is already RESOLVED");
        }

        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setResolvedAt(LocalDateTime.now());
        inc = incidentRepository.save(inc);

        incidentEventRepository.save(new IncidentEvent(inc, "RESOLVED", moderator, note != null ? note : "Incident resolved by moderator"));

        // Restore segment status if no other active incident on the segment
        RoadSegment seg = inc.getSegment();
        if (seg != null) {
            List<Incident> remainingActive = incidentRepository.findBySegmentIdAndStatus(seg.getId(), IncidentStatus.VERIFIED);

            if (remainingActive.isEmpty()) {
                SegmentStatus oldStatus = seg.getStatus();
                SegmentStatus restoredStatus = SegmentStatus.OPEN;

                List<RoadStatusHistory> historyList = roadStatusHistoryRepository.findBySegmentIdOrderByChangedAtDesc(seg.getId());
                if (!historyList.isEmpty()) {
                    for (RoadStatusHistory hist : historyList) {
                        if (hist.getOldStatus() != null && hist.getOldStatus() != SegmentStatus.BLOCKED && hist.getOldStatus() != SegmentStatus.RESTRICTED) {
                            restoredStatus = hist.getOldStatus();
                            break;
                        }
                    }
                }

                if (oldStatus != restoredStatus) {
                    seg.setStatus(restoredStatus);
                    seg.setStatusReason("Incident #" + inc.getId() + " resolved");
                    roadSegmentRepository.save(seg);
                    roadStatusHistoryRepository.save(new RoadStatusHistory(seg, oldStatus, restoredStatus, seg.getStatusReason(), moderator));
                    sseService.broadcast("road.updated", Map.of("segmentId", seg.getId(), "status", seg.getStatus()));
                }
            }
            riskEngine.computeSegmentRisk(seg);
        }

        auditLogService.recordAudit(moderator, "INCIDENT_RESOLVED", "INCIDENT", inc.getId(), "Resolved incident", "127.0.0.1");
        sseService.broadcast("incident.updated", Map.of("id", inc.getId(), "status", inc.getStatus()));

        return IncidentDto.fromEntity(inc);
    }

    public List<IncidentEventDto> getIncidentEvents(Long incidentId) {
        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByAtDesc(incidentId);
        return events.stream().map(IncidentEventDto::fromEntity).toList();
    }

    public Page<IncidentEventDto> getIncidentHistoryTimeline(Pageable pageable) {
        Page<IncidentEvent> page = incidentEventRepository.findAll(pageable);
        List<IncidentEventDto> dtos = page.getContent().stream().map(IncidentEventDto::fromEntity).toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }
}
