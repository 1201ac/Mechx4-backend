package com.nerlogix.service;

import com.nerlogix.dto.DispatchAssignmentDto;
import com.nerlogix.dto.DriverCandidateDto;
import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.ShipmentAssignment;
import com.nerlogix.entity.User;
import com.nerlogix.entity.Vehicle;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.Role;
import com.nerlogix.model.enums.ShipmentAssignmentStatus;
import com.nerlogix.model.enums.ShipmentStatus;
import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.repository.ShipmentAssignmentRepository;
import com.nerlogix.repository.ShipmentRepository;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import com.nerlogix.dto.DriverListItemDto;
import com.nerlogix.entity.Notification;
import com.nerlogix.repository.NotificationRepository;

@Service
public class DispatchService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final SseService sseService;

    public DispatchService(
            ShipmentRepository shipmentRepository,
            ShipmentAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            NotificationRepository notificationRepository,
            AuditLogService auditLogService,
            SseService sseService) {
        this.shipmentRepository = shipmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
        this.sseService = sseService;
    }

    @Transactional(readOnly = true)
    public List<DriverCandidateDto> findCandidates(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        double originLat = shipment.getOriginNode().getLat();
        double originLng = shipment.getOriginNode().getLng();
        double weightKg = shipment.getWeightKg() != null ? shipment.getWeightKg() : 1000.0;

        List<User> drivers = userRepository.findByRole(Role.USER);
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        List<DriverCandidateDto> candidates = new ArrayList<>();

        for (User driver : drivers) {
            // Check if driver currently has an active in-transit shipment
            boolean isBusy = shipmentRepository.findFirstByDriverUserIdAndStatusIn(
                    driver.getId(),
                    List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED)
            ).isPresent();

            // Find vehicles: driver-owned first, otherwise idle unassigned vehicles
            List<Vehicle> owned = allVehicles.stream()
                    .filter(v -> v.getOwnerUser() != null && v.getOwnerUser().getId().equals(driver.getId()))
                    .toList();

            List<Vehicle> eligibleVehicles = !owned.isEmpty()
                    ? owned
                    : allVehicles.stream().filter(v -> v.getStatus() == VehicleStatus.IDLE && v.getOwnerUser() == null).toList();

            for (Vehicle v : eligibleVehicles) {
                if (v.getCapacityKg() < weightKg) {
                    continue; // Disqualified: cannot carry payload
                }

                double distKm = haversineDistance(v.getLat(), v.getLng(), originLat, originLng);
                int score = 0;
                List<String> reasons = new ArrayList<>();

                // 1. Proximity Score (up to 40 pts)
                if (distKm <= 25.0) {
                    score += 40;
                    reasons.add(String.format("Very close to pickup (%.1f km)", distKm));
                } else if (distKm <= 75.0) {
                    score += 30;
                    reasons.add(String.format("Within region (%.1f km)", distKm));
                } else if (distKm <= 150.0) {
                    score += 20;
                    reasons.add(String.format("Accessible corridor (%.1f km)", distKm));
                } else {
                    score += 10;
                    reasons.add(String.format("Distance: %.1f km", distKm));
                }

                // 2. Capacity Utilization Score (up to 35 pts)
                double utilRatio = weightKg / v.getCapacityKg();
                if (utilRatio >= 0.65 && utilRatio <= 0.95) {
                    score += 35;
                    reasons.add(String.format("Optimal cargo utilization (%.0f%%)", utilRatio * 100));
                } else if (utilRatio < 0.65) {
                    score += 20;
                    reasons.add(String.format("Adequate capacity (%.0f%% filled)", utilRatio * 100));
                } else {
                    score += 25;
                    reasons.add(String.format("Near capacity limit (%.0f%% filled)", utilRatio * 100));
                }

                // 3. Driver & Vehicle Availability (up to 25 pts)
                if (!isBusy && v.getStatus() == VehicleStatus.IDLE) {
                    score += 25;
                    reasons.add("Driver and vehicle currently idle and available");
                } else if (isBusy) {
                    score = Math.max(10, score - 30);
                    reasons.add("Driver currently on another assignment");
                }

                int finalScore = Math.min(100, Math.max(5, score));

                candidates.add(new DriverCandidateDto(
                        driver.getId(),
                        driver.getName(),
                        driver.getEmail(),
                        v.getId(),
                        v.getRegNo(),
                        v.getType().name(),
                        v.getCapacityKg(),
                        Math.round(distKm * 10.0) / 10.0,
                        finalScore,
                        reasons
                ));
            }
        }

        candidates.sort(Comparator.comparingInt(DriverCandidateDto::matchScore).reversed());
        return candidates;
    }

    @Transactional
    public DispatchAssignmentDto assignShipment(Long shipmentId, Long driverUserId, Long vehicleId, User moderator) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT || shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipment is already " + shipment.getStatus());
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        // Vehicle validation
        if (vehicle.getStatus() != VehicleStatus.IDLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle " + vehicle.getRegNo() + " is currently not idle: " + vehicle.getStatus());
        }

        double payloadWeight = shipment.getWeightKg() != null ? shipment.getWeightKg() : 0.0;
        if (vehicle.getCapacityKg() < payloadWeight) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle capacity (" + vehicle.getCapacityKg() + " kg) is less than shipment weight (" + payloadWeight + " kg)");
        }

        // Determine driver: if driverUserId is provided, use it; otherwise check vehicle owner
        final Long effectiveDriverId;
        if (driverUserId != null) {
            effectiveDriverId = driverUserId;
        } else if (vehicle.getOwnerUser() != null) {
            effectiveDriverId = vehicle.getOwnerUser().getId();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fleet vehicle with no owner requires an explicit driver selection");
        }

        User driver = userRepository.findById(effectiveDriverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found: " + effectiveDriverId));

        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver " + driver.getEmail() + " is deactivated");
        }

        // If vehicle has an owner, check driver matches
        if (vehicle.getOwnerUser() != null && !vehicle.getOwnerUser().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle " + vehicle.getRegNo() + " belongs to user " + vehicle.getOwnerUser().getEmail() + ", cannot assign to driver " + driver.getEmail());
        }

        // Driver conflicting active job check
        Optional<Shipment> activeJob = shipmentRepository.findFirstByDriverUserIdAndStatusIn(
                driver.getId(),
                List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED)
        );
        if (activeJob.isPresent() && !activeJob.get().getId().equals(shipment.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver " + driver.getName() + " already has active in-transit shipment: " + activeJob.get().getCode());
        }

        // Cancel previous pending assignments for this shipment if reassigning
        List<ShipmentAssignment> existing = assignmentRepository.findByShipmentId(shipment.getId());
        for (ShipmentAssignment old : existing) {
            if (old.getStatus() == ShipmentAssignmentStatus.PENDING) {
                old.setStatus(ShipmentAssignmentStatus.CANCELLED);
                old.setRespondedAt(LocalDateTime.now());
                assignmentRepository.save(old);
            }
        }

        // Create assignment record
        ShipmentAssignment assignment = new ShipmentAssignment(shipment, driver, vehicle, moderator);
        assignment = assignmentRepository.save(assignment);

        // Update shipment placeholder
        shipment.setDriverUser(driver);
        shipment.setVehicle(vehicle);
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        shipmentRepository.save(shipment);

        // Create database notification row for driver
        Notification notif = new Notification(
                driver,
                null,
                "New Shipment Assignment: " + shipment.getCode(),
                "You have been assigned shipment " + shipment.getCode() + " (" + payloadWeight + " kg) with vehicle " + vehicle.getRegNo() + "."
        );
        notificationRepository.save(notif);

        // Record audit log entry
        String assignerName = moderator != null ? moderator.getEmail() : "SYSTEM";
        auditLogService.recordAudit(
                moderator,
                "SHIPMENT_ASSIGNED",
                "SHIPMENT",
                shipment.getId(),
                "Assigned shipment " + shipment.getCode() + " to driver " + driver.getEmail() + " and vehicle " + vehicle.getRegNo() + " by " + assignerName,
                "127.0.0.1"
        );

        DispatchAssignmentDto dto = DispatchAssignmentDto.fromEntity(assignment);

        // Notify specific driver via SSE
        sseService.sendToUser(driver.getId(), "dispatch.assigned", dto);
        sseService.sendToUser(driver.getId(), "alert.created", Map.of(
                "id", notif.getId(),
                "message", notif.getMessage(),
                "title", notif.getTitle()
        ));

        // Notify moderators / admins
        sseService.sendToRole("dispatch.assigned", dto, Role.MODERATOR, Role.ADMIN);
        sseService.broadcast("shipment.updated", Map.of("id", shipment.getId(), "status", shipment.getStatus()));

        return dto;
    }

    @Transactional
    public void unassignShipment(Long shipmentId, User moderator) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        if (shipment.getStatus() != ShipmentStatus.ASSIGNED && shipment.getStatus() != ShipmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot unassign shipment in status: " + shipment.getStatus());
        }

        // Cancel any pending/accepted assignment records
        List<ShipmentAssignment> assignments = assignmentRepository.findByShipmentId(shipmentId);
        for (ShipmentAssignment a : assignments) {
            if (a.getStatus() == ShipmentAssignmentStatus.PENDING || a.getStatus() == ShipmentAssignmentStatus.ACCEPTED) {
                a.setStatus(ShipmentAssignmentStatus.CANCELLED);
                a.setRespondedAt(LocalDateTime.now());
                assignmentRepository.save(a);
            }
        }

        User previousDriver = shipment.getDriverUser();
        Vehicle previousVehicle = shipment.getVehicle();

        shipment.setDriverUser(null);
        shipment.setVehicle(null);
        shipment.setStatus(ShipmentStatus.DRAFT);
        shipmentRepository.save(shipment);

        if (previousVehicle != null) {
            previousVehicle.setStatus(VehicleStatus.IDLE);
            vehicleRepository.save(previousVehicle);
        }

        auditLogService.recordAudit(
                moderator,
                "SHIPMENT_UNASSIGNED",
                "SHIPMENT",
                shipment.getId(),
                "Unassigned shipment " + shipment.getCode() + " (was assigned to driver " +
                        (previousDriver != null ? previousDriver.getEmail() : "none") + ")",
                "127.0.0.1"
        );

        Map<String, Object> eventData = Map.of(
                "shipmentId", shipment.getId(),
                "status", "DRAFT"
        );
        sseService.sendToRole("dispatch.declined", eventData, Role.MODERATOR, Role.ADMIN);
        sseService.broadcast("shipment.updated", Map.of("id", shipment.getId(), "status", shipment.getStatus()));
    }

    @Transactional
    public DispatchAssignmentDto acceptAssignment(Long assignmentId, User driver) {
        ShipmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (!assignment.getDriverUser().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to accept this assignment");
        }

        if (assignment.getStatus() != ShipmentAssignmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not pending: " + assignment.getStatus());
        }

        assignment.setStatus(ShipmentAssignmentStatus.ACCEPTED);
        assignment.setRespondedAt(LocalDateTime.now());
        assignment = assignmentRepository.save(assignment);

        // Ensure vehicle & shipment are in sync
        Shipment s = assignment.getShipment();
        s.setStatus(ShipmentStatus.ASSIGNED);
        s.setDriverUser(driver);
        s.setVehicle(assignment.getVehicle());
        shipmentRepository.save(s);

        auditLogService.recordAudit(
                driver,
                "DISPATCH_ACCEPTED",
                "SHIPMENT",
                s.getId(),
                "Driver " + driver.getEmail() + " accepted assignment for shipment " + s.getCode(),
                "127.0.0.1"
        );

        DispatchAssignmentDto dto = DispatchAssignmentDto.fromEntity(assignment);

        // Broadcast accept event
        sseService.sendToRole("dispatch.accepted", dto, Role.MODERATOR, Role.ADMIN);
        sseService.sendToUser(driver.getId(), "dispatch.accepted", dto);
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));

        return dto;
    }

    @Transactional
    public DispatchAssignmentDto declineAssignment(Long assignmentId, String reason, User driver) {
        ShipmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (!assignment.getDriverUser().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to decline this assignment");
        }

        if (assignment.getStatus() != ShipmentAssignmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not pending");
        }

        assignment.setStatus(ShipmentAssignmentStatus.DECLINED);
        assignment.setDeclineReason(reason != null ? reason : "Declined by driver");
        assignment.setRespondedAt(LocalDateTime.now());
        assignment = assignmentRepository.save(assignment);

        // Revert shipment back to DRAFT for redispatch
        Shipment s = assignment.getShipment();
        s.setStatus(ShipmentStatus.DRAFT);
        s.setDriverUser(null);
        s.setVehicle(null);
        shipmentRepository.save(s);

        auditLogService.recordAudit(
                driver,
                "DISPATCH_DECLINED",
                "SHIPMENT",
                s.getId(),
                "Driver " + driver.getEmail() + " declined assignment for shipment " + s.getCode() + ". Reason: " + assignment.getDeclineReason(),
                "127.0.0.1"
        );

        DispatchAssignmentDto dto = DispatchAssignmentDto.fromEntity(assignment);

        // Broadcast decline event
        sseService.sendToRole("dispatch.declined", dto, Role.MODERATOR, Role.ADMIN);
        sseService.sendToUser(driver.getId(), "dispatch.declined", dto);
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<DispatchAssignmentDto> getPendingAssignmentsForDriver(User driver) {
        List<ShipmentAssignment> list = assignmentRepository.findByDriverUserIdAndStatus(
                driver.getId(),
                ShipmentAssignmentStatus.PENDING
        );
        return list.stream().map(DispatchAssignmentDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDispatchBoard() {
        // Unassigned or DRAFT shipments
        List<Shipment> unassigned = shipmentRepository.findByStatus(ShipmentStatus.DRAFT);
        // Active assignments
        List<ShipmentAssignment> pending = assignmentRepository.findByStatus(ShipmentAssignmentStatus.PENDING);
        List<ShipmentAssignment> accepted = assignmentRepository.findByStatus(ShipmentAssignmentStatus.ACCEPTED);

        return Map.of(
                "unassignedShipments", unassigned.stream().map(com.nerlogix.dto.ShipmentDto::fromEntity).toList(),
                "pendingAssignments", pending.stream().map(DispatchAssignmentDto::fromEntity).toList(),
                "recentAccepted", accepted.stream().map(DispatchAssignmentDto::fromEntity).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<DriverListItemDto> getDriversList(String search) {
        List<User> drivers = userRepository.findByRole(Role.USER);
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        Map<Long, List<Vehicle>> vehiclesByOwner = new HashMap<>();
        for (Vehicle v : allVehicles) {
            if (v.getOwnerUser() != null) {
                vehiclesByOwner.computeIfAbsent(v.getOwnerUser().getId(), k -> new ArrayList<>()).add(v);
            }
        }

        List<DriverListItemDto> results = new ArrayList<>();
        String query = search != null ? search.trim().toLowerCase() : "";

        for (User d : drivers) {
            List<Vehicle> owned = vehiclesByOwner.getOrDefault(d.getId(), Collections.emptyList());
            List<String> regNos = owned.stream().map(Vehicle::getRegNo).toList();

            // Match search filter
            if (!query.isBlank()) {
                boolean matchName = d.getName() != null && d.getName().toLowerCase().contains(query);
                boolean matchEmail = d.getEmail() != null && d.getEmail().toLowerCase().contains(query);
                boolean matchReg = regNos.stream().anyMatch(r -> r.toLowerCase().contains(query));
                if (!matchName && !matchEmail && !matchReg) {
                    continue;
                }
            }

            // Current job lookup
            Optional<Shipment> activeShipment = shipmentRepository.findFirstByDriverUserIdAndStatusIn(
                    d.getId(),
                    List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED)
            );
            String currentJob = activeShipment.map(s -> s.getCode() + " (" + s.getStatus() + ")").orElse("Idle / Available");

            String vType = owned.isEmpty() ? "None" : owned.get(0).getType().name();
            Double vCapacity = owned.isEmpty() ? null : owned.get(0).getCapacityKg();

            results.add(new DriverListItemDto(
                    d.getId(),
                    d.getName(),
                    d.getEmail(),
                    d.getActive(),
                    regNos,
                    vType,
                    vCapacity,
                    currentJob
            ));
        }

        return results;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
