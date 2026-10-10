package com.nerlogix.controller;

import com.nerlogix.dto.*;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.CargoCategory;
import com.nerlogix.model.enums.ShipmentPriority;
import com.nerlogix.model.enums.ShipmentStatus;
import com.nerlogix.service.ShipmentService;
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

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public ResponseEntity<Page<ShipmentDto>> getShipments(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) Long driverUserId,
            @RequestParam(required = false) CargoCategory cargoCategory,
            @RequestParam(required = false) ShipmentPriority priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "desc"), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(shipmentService.getShipments(currentUser, status, driverUserId, cargoCategory, priority, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDto> getShipmentById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id, currentUser));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ShipmentDto> createShipment(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateShipmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.createShipment(req, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ShipmentDto> updateShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateShipmentRequest req) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, req, currentUser));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ShipmentDto> assignShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AssignShipmentRequest req) {
        return ResponseEntity.ok(shipmentService.assignShipment(id, req, currentUser));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ShipmentDto> startShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(shipmentService.startShipment(id, currentUser));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ShipmentDto> pauseShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(shipmentService.pauseShipment(id, currentUser));
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<ShipmentDto> deliverShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(shipmentService.deliverShipment(id, currentUser));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ShipmentDto> cancelShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(shipmentService.cancelShipment(id, currentUser));
    }

    @GetMapping("/{id}/route-proposal")
    public ResponseEntity<RoutePlanDto> getRouteProposal(@PathVariable Long id) {
        RoutePlanDto proposal = shipmentService.getRouteProposal(id);
        if (proposal == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(proposal);
    }

    @PostMapping("/{id}/route-proposal/accept")
    public ResponseEntity<RoutePlanDto> acceptRouteProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(shipmentService.acceptRouteProposal(id, currentUser));
    }

    @PostMapping("/{id}/route-proposal/reject")
    public ResponseEntity<RoutePlanDto> rejectRouteProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(shipmentService.rejectRouteProposal(id, currentUser));
    }
}
