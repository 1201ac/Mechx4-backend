package com.nerlogix.dto;

import com.nerlogix.entity.FieldReport;
import com.nerlogix.entity.ReportImage;
import com.nerlogix.model.enums.IncidentType;
import com.nerlogix.model.enums.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;

public record FieldReportDto(
    Long id,
    String clientId,
    UserDto user,
    Double lat,
    Double lng,
    Double accuracy,
    LocalDateTime capturedAt,
    IncidentType type,
    Integer severity,
    String note,
    ReportStatus status,
    String moderatorNote,
    Long incidentId,
    List<String> imageUrls,
    LocalDateTime createdAt
) {
    public static FieldReportDto fromEntity(FieldReport r, List<ReportImage> images) {
        List<String> urls = images != null ? images.stream().map(ReportImage::getFilePath).toList() : List.of();
        return new FieldReportDto(
            r.getId(),
            r.getClientId(),
            r.getUser() != null ? UserDto.fromEntity(r.getUser()) : null,
            r.getLat(),
            r.getLng(),
            r.getAccuracy(),
            r.getCapturedAt(),
            r.getType(),
            r.getSeverity(),
            r.getNote(),
            r.getStatus(),
            r.getModeratorNote(),
            r.getIncident() != null ? r.getIncident().getId() : null,
            urls,
            r.getCreatedAt()
        );
    }
}
