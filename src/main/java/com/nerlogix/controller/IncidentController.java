package com.nerlogix.controller;

import com.nerlogix.dto.*;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.IncidentStatus;
import com.nerlogix.model.enums.IncidentType;
import com.nerlogix.service.IncidentService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<Page<IncidentDto>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) Long segmentId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(incidentService.getIncidents(status, type, districtId, segmentId, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> getIncidentById(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentById(id));
    }

    @PostMapping
    public ResponseEntity<IncidentDto> createIncident(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateIncidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(req, currentUser));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<IncidentDto> resolveIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, String> body) {
        String note = (body != null && body.containsKey("note")) ? body.get("note") : null;
        return ResponseEntity.ok(incidentService.resolveIncident(id, note, currentUser));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<IncidentEventDto>> getIncidentEvents(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncidentEvents(id));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<IncidentEventDto>> getIncidentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "at"));
        return ResponseEntity.ok(incidentService.getIncidentHistoryTimeline(pageable));
    }
}
