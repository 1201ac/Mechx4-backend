package com.nerlogix.controller;

import com.nerlogix.dto.RegisterUserVehicleRequest;
import com.nerlogix.dto.UpdateVehicleRequest;
import com.nerlogix.dto.VehicleDto;
import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.User;
import com.nerlogix.entity.Vehicle;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.Role;
import com.nerlogix.model.enums.ShipmentStatus;
import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.repository.ShipmentRepository;
import com.nerlogix.repository.VehicleRepository;
import com.nerlogix.service.SseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me/vehicles")
@PreAuthorize("isAuthenticated()")
public class UserVehicleController {

    private final VehicleRepository vehicleRepository;
    private final ShipmentRepository shipmentRepository;
    private final SseService sseService;

    public UserVehicleController(
            VehicleRepository vehicleRepository,
            ShipmentRepository shipmentRepository,
            SseService sseService) {
        this.vehicleRepository = vehicleRepository;
        this.shipmentRepository = shipmentRepository;
        this.sseService = sseService;
    }

    @GetMapping
    public ResponseEntity<List<VehicleDto>> getMyVehicles(@AuthenticationPrincipal User currentUser) {
        List<Vehicle> vehicles = vehicleRepository.findByOwnerUserId(currentUser.getId());
        List<VehicleDto> dtos = vehicles.stream().map(VehicleDto::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<VehicleDto> registerMyVehicle(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RegisterUserVehicleRequest req) {

        String regNo = req.regNo().trim().toUpperCase();
        if (vehicleRepository.findByRegNo(regNo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle registration number already exists: " + regNo);
        }

        double lat = req.lat() != null ? req.lat() : 26.1445; // Default Guwahati HQ
        double lng = req.lng() != null ? req.lng() : 91.7362;

        Vehicle vehicle = new Vehicle(regNo, req.type(), req.capacityKg(), lat, lng);
        vehicle.setStatus(VehicleStatus.IDLE);
        vehicle.setOwnerUser(currentUser);
        vehicle.setLastSeen(LocalDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);

        sseService.sendToRole("vehicle.update", Map.of(
                "vehicleId", saved.getId(),
                "regNo", saved.getRegNo(),
                "status", saved.getStatus(),
                "ownerUserId", currentUser.getId()
        ), Role.MODERATOR, Role.ADMIN);

        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleDto.fromEntity(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleDto> updateMyVehicle(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateVehicleRequest req) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));

        boolean isOwner = vehicle.getOwnerUser() != null && vehicle.getOwnerUser().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MODERATOR;

        if (!isOwner && !isStaff) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this vehicle");
        }

        if (req.regNo() != null && !req.regNo().isBlank()) {
            vehicle.setRegNo(req.regNo().trim().toUpperCase());
        }
        if (req.type() != null) vehicle.setType(req.type());
        if (req.capacityKg() != null) vehicle.setCapacityKg(req.capacityKg());
        if (req.status() != null) vehicle.setStatus(req.status());
        if (req.lat() != null) vehicle.setLat(req.lat());
        if (req.lng() != null) vehicle.setLng(req.lng());
        vehicle.setLastSeen(LocalDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(VehicleDto.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyVehicle(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));

        boolean isOwner = vehicle.getOwnerUser() != null && vehicle.getOwnerUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this vehicle");
        }

        List<Shipment> activeShipments = shipmentRepository.findByVehicleIdAndStatusIn(id, List.of(
                ShipmentStatus.ASSIGNED,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.DELAYED
        ));

        if (!activeShipments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete vehicle with active shipments");
        }

        vehicleRepository.delete(vehicle);
        return ResponseEntity.noContent().build();
    }
}
