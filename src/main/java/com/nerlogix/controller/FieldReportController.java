package com.nerlogix.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.FieldReportDto;
import com.nerlogix.dto.FieldReportPayload;
import com.nerlogix.dto.VerifyReportRequest;
import com.nerlogix.entity.User;
import com.nerlogix.service.FieldReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/field-reports")
public class FieldReportController {

    private final FieldReportService fieldReportService;
    private final ObjectMapper objectMapper;

    public FieldReportController(FieldReportService fieldReportService, ObjectMapper objectMapper) {
        this.fieldReportService = fieldReportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createFieldReport(
            @AuthenticationPrincipal User currentUser,
            @RequestPart("payload") String payloadJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            FieldReportPayload payload = objectMapper.readValue(payloadJson, FieldReportPayload.class);
            Map<String, Object> result = fieldReportService.createFieldReport(payload, images, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            throw new RuntimeException("Invalid field report payload: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<FieldReportDto>> getFieldReports(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(fieldReportService.getFieldReports(currentUser, pageable));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<FieldReportDto> verifyReport(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) VerifyReportRequest req) {
        String note = req != null ? req.moderatorNote() : null;
        return ResponseEntity.ok(fieldReportService.verifyReport(id, note, currentUser));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<FieldReportDto> rejectReport(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) VerifyReportRequest req) {
        String note = req != null ? req.moderatorNote() : null;
        return ResponseEntity.ok(fieldReportService.rejectReport(id, note, currentUser));
    }
}
