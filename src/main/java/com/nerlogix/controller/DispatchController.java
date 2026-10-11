package com.nerlogix.controller;

import com.nerlogix.dto.AssignShipmentRequest;
import com.nerlogix.dto.DeclineAssignmentRequest;
import com.nerlogix.dto.DispatchAssignmentDto;
import com.nerlogix.dto.DriverCandidateDto;
import com.nerlogix.entity.User;
import com.nerlogix.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<DriverCandidateDto>> getCandidates(@RequestParam Long shipmentId) {
        return ResponseEntity.ok(dispatchService.findCandidates(shipmentId));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<DispatchAssignmentDto> assignShipment(
            @AuthenticationPrincipal User moderator,
            @Valid @RequestBody AssignShipmentRequest req) {
        return ResponseEntity.ok(dispatchService.assignShipment(
                req.shipmentId(),
                req.driverUserId(),
                req.vehicleId(),
                moderator
        ));
    }

    @GetMapping("/board")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDispatchBoard() {
        return ResponseEntity.ok(dispatchService.getDispatchBoard());
    }

    @PostMapping("/accept/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DispatchAssignmentDto> acceptAssignment(
            @PathVariable Long id,
            @AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(dispatchService.acceptAssignment(id, driver));
    }

    @PostMapping("/decline/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DispatchAssignmentDto> declineAssignment(
            @PathVariable Long id,
            @RequestBody(required = false) DeclineAssignmentRequest req,
            @AuthenticationPrincipal User driver) {
        String reason = req != null ? req.reason() : null;
        return ResponseEntity.ok(dispatchService.declineAssignment(id, reason, driver));
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DispatchAssignmentDto>> getPendingAssignments(
            @AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(dispatchService.getPendingAssignmentsForDriver(driver));
    }
}
