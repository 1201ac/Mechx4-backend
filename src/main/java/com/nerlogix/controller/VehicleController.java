package com.nerlogix.controller;

import com.nerlogix.dto.*;
import com.nerlogix.entity.User;
import com.nerlogix.service.VehicleService;
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

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ResponseEntity<Page<VehicleDto>> getVehicles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort sortObj = Sort.by(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "asc"), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(vehicleService.getVehicles(status, type, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDto> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<VehicleDto> createVehicle(@Valid @RequestBody CreateVehicleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.createVehicle(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<VehicleDto> updateVehicle(@PathVariable Long id, @Valid @RequestBody UpdateVehicleRequest req) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/trail")
    public ResponseEntity<List<VehiclePositionDto>> getVehicleTrail(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleTrail(id));
    }

    @PostMapping("/me/position")
    public ResponseEntity<Void> recordPosition(
            @AuthenticationPrincipal User currentUser,
            @RequestBody List<VehiclePositionRequest> positions) {
        vehicleService.recordDriverPositions(currentUser, positions);
        return ResponseEntity.ok().build();
    }
}
