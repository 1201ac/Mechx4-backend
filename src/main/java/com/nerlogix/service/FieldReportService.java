package com.nerlogix.service;

import com.nerlogix.config.NerlogixProperties;
import com.nerlogix.dto.FieldReportDto;
import com.nerlogix.dto.FieldReportPayload;
import com.nerlogix.entity.*;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;
import com.nerlogix.service.risk.RiskEngine;
import com.nerlogix.util.GeoUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FieldReportService {

    private final FieldReportRepository fieldReportRepository;
    private final ReportImageRepository reportImageRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadStatusHistoryRepository roadStatusHistoryRepository;
    private final DistrictRepository districtRepository;
    private final AuditLogService auditLogService;
    private final RiskEngine riskEngine;
    private final SseService sseService;
    private final NerlogixProperties nerlogixProperties;

    public FieldReportService(
            FieldReportRepository fieldReportRepository,
            ReportImageRepository reportImageRepository,
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            RoadSegmentRepository roadSegmentRepository,
            RoadStatusHistoryRepository roadStatusHistoryRepository,
            DistrictRepository districtRepository,
            AuditLogService auditLogService,
            RiskEngine riskEngine,
            SseService sseService,
            NerlogixProperties nerlogixProperties) {
        this.fieldReportRepository = fieldReportRepository;
        this.reportImageRepository = reportImageRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadStatusHistoryRepository = roadStatusHistoryRepository;
        this.districtRepository = districtRepository;
        this.auditLogService = auditLogService;
        this.riskEngine = riskEngine;
        this.sseService = sseService;
        this.nerlogixProperties = nerlogixProperties;
    }

    @Transactional
    public Map<String, Object> createFieldReport(FieldReportPayload payload, List<MultipartFile> images, User user) {
        Optional<FieldReport> existing = fieldReportRepository.findByClientId(payload.clientId());
        if (existing.isPresent()) {
            FieldReport r = existing.get();
            List<ReportImage> rImages = reportImageRepository.findByFieldReportId(r.getId());
            return Map.of("duplicate", true, "report", FieldReportDto.fromEntity(r, rImages));
        }

        if (images != null && images.size() > 5) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum 5 images allowed per report");
        }

        FieldReport fr = new FieldReport();
        fr.setClientId(payload.clientId());
        fr.setUser(user);
        fr.setLat(payload.lat());
        fr.setLng(payload.lng());
        fr.setAccuracy(payload.accuracy());
        fr.setCapturedAt(payload.capturedAt() != null ? payload.capturedAt() : LocalDateTime.now());
        fr.setType(payload.type());
        fr.setSeverity(payload.severity());
        fr.setNote(payload.note());
        fr.setStatus(ReportStatus.PENDING);

        try {
            fr = fieldReportRepository.save(fr);
        } catch (DataIntegrityViolationException ex) {
            Optional<FieldReport> dup = fieldReportRepository.findByClientId(payload.clientId());
            if (dup.isPresent()) {
                FieldReport r = dup.get();
                List<ReportImage> rImages = reportImageRepository.findByFieldReportId(r.getId());
                return Map.of("duplicate", true, "report", FieldReportDto.fromEntity(r, rImages));
            }
            throw ex;
        }

        List<ReportImage> reportImages = new ArrayList<>();
        File uploadFolder = new File(nerlogixProperties.getUploads().getDir());
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                String ext = validateAndGetExtension(file);
                String filename = "report_" + UUID.randomUUID() + ext;
                File targetFile = new File(uploadFolder, filename);
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    fos.write(file.getBytes());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save image file: " + e.getMessage());
                }
                ReportImage img = reportImageRepository.save(new ReportImage(fr, "/api/uploads/" + filename));
                reportImages.add(img);
            }
        }

        auditLogService.recordAudit(user, "FIELD_REPORT_SUBMITTED", "FIELD_REPORT", fr.getId(), "Submitted field report for " + fr.getType(), "127.0.0.1");

        FieldReportDto dto = FieldReportDto.fromEntity(fr, reportImages);
        return Map.of("duplicate", false, "report", dto);
    }

    public Page<FieldReportDto> getFieldReports(User currentUser, Pageable pageable) {
        Page<FieldReport> page;
        if (currentUser.getRole() == Role.USER) {
            page = fieldReportRepository.findByUserId(currentUser.getId(), pageable);
        } else {
            page = fieldReportRepository.findAll(pageable);
        }

        List<FieldReportDto> dtos = page.getContent().stream().map(r -> {
            List<ReportImage> imgs = reportImageRepository.findByFieldReportId(r.getId());
            return FieldReportDto.fromEntity(r, imgs);
        }).toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional
    public FieldReportDto verifyReport(Long id, String moderatorNote, User moderator) {
        FieldReport fr = fieldReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field report not found: " + id));

        if (fr.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report is already " + fr.getStatus());
        }

        fr.setStatus(ReportStatus.VERIFIED);
        fr.setModeratorNote(moderatorNote);

        RoadSegment nearestSeg = findNearestSegment(fr.getLat(), fr.getLng());
        District nearestDist = findNearestDistrict(fr.getLat(), fr.getLng());

        final IncidentType reportType = fr.getType();
        final double reportLat = fr.getLat();
        final double reportLng = fr.getLng();

        List<Incident> candidateIncidents = incidentRepository.findByTypeAndStatusNotAndCreatedAtAfter(
                reportType, IncidentStatus.RESOLVED, LocalDateTime.now().minusHours(6)
        );
        List<Incident> openIncidents = candidateIncidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.REJECTED)
                .filter(i -> GeoUtils.haversineDistanceKm(i.getLat(), i.getLng(), reportLat, reportLng) <= 2.0)
                .toList();

        Incident incident;
        if (!openIncidents.isEmpty()) {
            incident = openIncidents.get(0);
            if (fr.getSeverity() > incident.getSeverity()) {
                incident.setSeverity(fr.getSeverity());
            }
            String newDesc = incident.getDescription() + " | Verified report: " + (fr.getNote() != null ? fr.getNote() : "");
            if (newDesc.length() > 500) {
                newDesc = newDesc.substring(0, 497) + "...";
            }
            incident.setDescription(newDesc);
            incident = incidentRepository.save(incident);
        } else {
            incident = new Incident();
            incident.setType(fr.getType());
            incident.setSeverity(fr.getSeverity());
            incident.setLat(fr.getLat());
            incident.setLng(fr.getLng());
            incident.setSegment(nearestSeg);
            incident.setDistrict(nearestDist);
            String desc = "Verified field report: " + (fr.getNote() != null ? fr.getNote() : "");
            if (desc.length() > 500) desc = desc.substring(0, 497) + "...";
            incident.setDescription(desc);
            incident.setStatus(IncidentStatus.VERIFIED);
            incident.setSource(IncidentSource.FIELD);
            incident.setReportId(fr.getId());
            incident.setCreatedBy(fr.getUser());
            incident = incidentRepository.save(incident);
        }

        fr.setIncident(incident);
        fr = fieldReportRepository.save(fr);

        incidentEventRepository.save(new IncidentEvent(incident, "VERIFIED", moderator, moderatorNote != null ? moderatorNote : "Report verified by moderator"));

        // Status rules based on max severity of merged incident
        int maxSeverity = incident.getSeverity();
        if (nearestSeg != null) {
            SegmentStatus oldStatus = nearestSeg.getStatus();
            if (maxSeverity == 3 && oldStatus != SegmentStatus.BLOCKED) {
                nearestSeg.setStatus(SegmentStatus.RESTRICTED);
                nearestSeg.setStatusReason("VERIFIED Incident: " + fr.getType() + " severity 3");
            } else if (maxSeverity >= 4 && isBlockingIncidentType(fr.getType())) {
                nearestSeg.setStatus(SegmentStatus.BLOCKED);
                nearestSeg.setStatusReason("VERIFIED Incident: " + fr.getType() + " severity 4");
            }

            if (nearestSeg.getStatus() != oldStatus) {
                roadSegmentRepository.save(nearestSeg);
                roadStatusHistoryRepository.save(new RoadStatusHistory(nearestSeg, oldStatus, nearestSeg.getStatus(), nearestSeg.getStatusReason(), moderator));
                sseService.broadcast("road.updated", Map.of("segmentId", nearestSeg.getId(), "status", nearestSeg.getStatus()));
            }

            riskEngine.computeSegmentRisk(nearestSeg);
        }

        auditLogService.recordAudit(moderator, "FIELD_REPORT_VERIFIED", "FIELD_REPORT", fr.getId(), "Verified field report", "127.0.0.1");

        List<ReportImage> imgs = reportImageRepository.findByFieldReportId(fr.getId());
        return FieldReportDto.fromEntity(fr, imgs);
    }

    @Transactional
    public FieldReportDto rejectReport(Long id, String moderatorNote, User moderator) {
        FieldReport fr = fieldReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Field report not found: " + id));

        if (fr.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report is already " + fr.getStatus());
        }

        fr.setStatus(ReportStatus.REJECTED);
        fr.setModeratorNote(moderatorNote);
        fr = fieldReportRepository.save(fr);

        auditLogService.recordAudit(moderator, "FIELD_REPORT_REJECTED", "FIELD_REPORT", fr.getId(), "Rejected field report", "127.0.0.1");

        List<ReportImage> imgs = reportImageRepository.findByFieldReportId(fr.getId());
        return FieldReportDto.fromEntity(fr, imgs);
    }

    private boolean isBlockingIncidentType(IncidentType type) {
        return type == IncidentType.LANDSLIDE || type == IncidentType.FLOOD ||
               type == IncidentType.BRIDGE_DAMAGE || type == IncidentType.ROAD_DAMAGE;
    }

    private RoadSegment findNearestSegment(double lat, double lng) {
        List<RoadSegment> segments = roadSegmentRepository.findAll();
        RoadSegment nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (RoadSegment s : segments) {
            double d1 = GeoUtils.haversineDistanceKm(lat, lng, s.getFromNode().getLat(), s.getFromNode().getLng());
            double d2 = GeoUtils.haversineDistanceKm(lat, lng, s.getToNode().getLat(), s.getToNode().getLng());
            double dist = Math.min(d1, d2);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = s;
            }
        }
        return nearest;
    }

    private District findNearestDistrict(double lat, double lng) {
        List<District> districts = districtRepository.findAll();
        District nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (District d : districts) {
            double dist = GeoUtils.haversineDistanceKm(lat, lng, d.getLat(), d.getLng());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = d;
            }
        }
        return nearest;
    }

    private String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) return ".png";

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds maximum allowed 5 MB");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header, 0, header.length);
            if (read < 4) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid or empty image file");
            }

            // Check JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return ".jpg";
            }

            // Check PNG: 89 50 4E 47 0D 0A 1A 0A
            if (read >= 8 &&
                (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47 &&
                header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
                return ".png";
            }

            // Check WEBP: RIFF....WEBP
            if (read >= 12 &&
                header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
                header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return ".webp";
            }

            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image type. Only JPEG, PNG, and WEBP images are allowed.");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image file header");
        }
    }
}
