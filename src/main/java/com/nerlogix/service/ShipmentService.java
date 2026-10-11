package com.nerlogix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.dto.*;
import com.nerlogix.entity.*;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;
import com.nerlogix.service.routing.RoutePlannerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final DistrictStockRepository districtStockRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RoutePlannerService routePlannerService;
    private final AuditLogService auditLogService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            RoadNodeRepository roadNodeRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            DistrictStockRepository districtStockRepository,
            RoutePlanRepository routePlanRepository,
            RoutePlannerService routePlannerService,
            AuditLogService auditLogService,
            SseService sseService,
            ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.districtStockRepository = districtStockRepository;
        this.routePlanRepository = routePlanRepository;
        this.routePlannerService = routePlannerService;
        this.auditLogService = auditLogService;
        this.sseService = sseService;
        this.objectMapper = objectMapper;
    }

    public Page<ShipmentDto> getShipments(
            User currentUser,
            ShipmentStatus status,
            Long driverUserId,
            CargoCategory cargoCategory,
            ShipmentPriority priority,
            String q,
            Pageable pageable) {

        boolean isUserOnly = currentUser != null && currentUser.getRole() == Role.USER;
        Long effectiveDriverId = isUserOnly ? currentUser.getId() : driverUserId;

        org.springframework.data.jpa.domain.Specification<Shipment> spec = 
                com.nerlogix.repository.specification.ShipmentSpecification.filter(
                        status,
                        effectiveDriverId,
                        cargoCategory,
                        priority,
                        q
                );

        return shipmentRepository.findAll(spec, pageable).map(ShipmentDto::fromEntity);
    }

    public ShipmentDto getShipmentById(Long id, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        if (currentUser != null && currentUser.getRole() == Role.USER) {
            if (s.getDriverUser() == null || !s.getDriverUser().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to shipment #" + id);
            }
        }

        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto createShipment(CreateShipmentRequest req, User currentUser) {
        RoadNode orig = roadNodeRepository.findById(req.originNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Origin node not found: " + req.originNodeId()));
        RoadNode dest = roadNodeRepository.findById(req.destinationNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination node not found: " + req.destinationNodeId()));

        String code = req.code();
        if (code == null || code.isBlank()) {
            code = "SHP-" + (System.currentTimeMillis() % 100000);
        }

        Shipment s = new Shipment();
        s.setCode(code);
        s.setCargoCategory(req.cargoCategory());
        s.setCargoDescription(req.cargoDescription());
        s.setWeightKg(req.weightKg());
        s.setPriority(req.priority() != null ? req.priority() : ShipmentPriority.NORMAL);
        s.setOriginNode(orig);
        s.setDestinationNode(dest);
        s.setPlannedEta(req.plannedEta() != null ? req.plannedEta() : LocalDateTime.now().plusHours(6));
        s.setCurrentEta(s.getPlannedEta());
        s.setStatus(ShipmentStatus.DRAFT);

        if (req.vehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + req.vehicleId()));
            s.setVehicle(v);
        }
        if (req.driverUserId() != null) {
            User driver = userRepository.findById(req.driverUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver user not found: " + req.driverUserId()));
            s.setDriverUser(driver);
        }

        if (s.getVehicle() != null && s.getDriverUser() != null) {
            s.setStatus(ShipmentStatus.ASSIGNED);
        }

        s = shipmentRepository.save(s);
        auditLogService.recordAudit(currentUser, "SHIPMENT_CREATED", "SHIPMENT", s.getId(), "Created shipment " + s.getCode(), "127.0.0.1");
        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto updateShipment(Long id, UpdateShipmentRequest req, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        if (req.cargoCategory() != null) s.setCargoCategory(req.cargoCategory());
        if (req.cargoDescription() != null) s.setCargoDescription(req.cargoDescription());
        if (req.weightKg() != null) s.setWeightKg(req.weightKg());
        if (req.priority() != null) s.setPriority(req.priority());

        if (req.originNodeId() != null) {
            RoadNode orig = roadNodeRepository.findById(req.originNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Origin node not found: " + req.originNodeId()));
            s.setOriginNode(orig);
        }
        if (req.destinationNodeId() != null) {
            RoadNode dest = roadNodeRepository.findById(req.destinationNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination node not found: " + req.destinationNodeId()));
            s.setDestinationNode(dest);
        }
        if (req.vehicleId() != null) {
            Vehicle v = vehicleRepository.findById(req.vehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + req.vehicleId()));
            s.setVehicle(v);
        }
        if (req.driverUserId() != null) {
            User driver = userRepository.findById(req.driverUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Driver user not found: " + req.driverUserId()));
            s.setDriverUser(driver);
        }
        if (req.plannedEta() != null) {
            s.setPlannedEta(req.plannedEta());
            s.setCurrentEta(req.plannedEta());
        }

        s = shipmentRepository.save(s);
        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto assignShipment(Long id, AssignShipmentRequest req, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        if (s.getStatus() != ShipmentStatus.DRAFT && s.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot assign shipment in status: " + s.getStatus());
        }

        Vehicle v = vehicleRepository.findById(req.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + req.vehicleId()));

        List<Shipment> activeVehicleShipments = shipmentRepository.findByVehicleIdAndStatusIn(
                v.getId(), List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED));
        for (Shipment active : activeVehicleShipments) {
            if (!active.getId().equals(s.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle " + v.getRegNo() + " is already assigned to active shipment " + active.getCode());
            }
        }

        User driver = userRepository.findById(req.driverUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found: " + req.driverUserId()));
        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver user is deactivated: " + driver.getEmail());
        }

        Optional<Shipment> activeDriverShipment = shipmentRepository.findByDriverUserIdAndStatusIn(
                driver.getId(), List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED));
        if (activeDriverShipment.isPresent() && !activeDriverShipment.get().getId().equals(s.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver " + driver.getEmail() + " is already assigned to active shipment " + activeDriverShipment.get().getCode());
        }

        s.setVehicle(v);
        s.setDriverUser(driver);
        s.setStatus(ShipmentStatus.ASSIGNED);

        // Compute ACTIVE route plan
        List<RouteOptionDto> routeOptions = routePlannerService.planRoutes(new RoutePlanRequest(
                s.getOriginNode().getId(),
                s.getDestinationNode().getId(),
                v.getType(),
                s.getPriority()
        ));

        if (routeOptions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No safe route path available for origin and destination");
        }

        RouteOptionDto bestOption = routeOptions.get(0);

        List<RoutePlan> activePlans = routePlanRepository.findByShipmentIdAndStatus(s.getId(), RoutePlanStatus.ACTIVE);
        for (RoutePlan oldPlan : activePlans) {
            oldPlan.setStatus(RoutePlanStatus.SUPERSEDED);
            routePlanRepository.save(oldPlan);
        }

        RoutePlan plan = new RoutePlan();
        plan.setShipment(s);
        try {
            plan.setSegmentIds(objectMapper.writeValueAsString(bestOption.segmentIds()));
        } catch (Exception e) {
            plan.setSegmentIds("[]");
        }
        plan.setGeometry(bestOption.geometry());
        plan.setDistanceKm(bestOption.distanceKm());
        plan.setEtaMin((double) bestOption.etaMinutes());
        plan.setRiskScore(bestOption.riskScore());
        plan.setStatus(RoutePlanStatus.ACTIVE);
        plan.setReason(bestOption.reason());
        routePlanRepository.save(plan);

        s.setPlannedEta(LocalDateTime.now().plusMinutes(bestOption.etaMinutes()));
        s.setCurrentEta(s.getPlannedEta());

        s = shipmentRepository.save(s);
        auditLogService.recordAudit(currentUser, "SHIPMENT_ASSIGNED", "SHIPMENT", s.getId(), "Assigned shipment " + s.getCode() + " to vehicle " + v.getRegNo(), "127.0.0.1");
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));
        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto startShipment(Long id, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        validateDriverOrModeratorAccess(s, currentUser);

        if (s.getStatus() != ShipmentStatus.ASSIGNED && s.getStatus() != ShipmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition from " + s.getStatus() + " to IN_TRANSIT");
        }

        if (s.getVehicle() == null || s.getDriverUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Starting shipment requires vehicle and driver assignment");
        }

        // If no ACTIVE plan exists, compute it now
        Optional<RoutePlan> activePlanOpt = routePlanRepository.findFirstByShipmentIdAndStatusOrderByIdDesc(s.getId(), RoutePlanStatus.ACTIVE);
        if (activePlanOpt.isEmpty()) {
            List<RouteOptionDto> routeOptions = routePlannerService.planRoutes(new RoutePlanRequest(
                    s.getOriginNode().getId(),
                    s.getDestinationNode().getId(),
                    s.getVehicle().getType(),
                    s.getPriority()
            ));
            if (routeOptions.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No safe route path available for origin and destination");
            }
            RouteOptionDto bestOption = routeOptions.get(0);
            RoutePlan plan = new RoutePlan();
            plan.setShipment(s);
            try {
                plan.setSegmentIds(objectMapper.writeValueAsString(bestOption.segmentIds()));
            } catch (Exception e) {
                plan.setSegmentIds("[]");
            }
            plan.setGeometry(bestOption.geometry());
            plan.setDistanceKm(bestOption.distanceKm());
            plan.setEtaMin((double) bestOption.etaMinutes());
            plan.setRiskScore(bestOption.riskScore());
            plan.setStatus(RoutePlanStatus.ACTIVE);
            plan.setReason(bestOption.reason());
            routePlanRepository.save(plan);

            s.setPlannedEta(LocalDateTime.now().plusMinutes(bestOption.etaMinutes()));
            s.setCurrentEta(s.getPlannedEta());
        }

        s.setStatus(ShipmentStatus.IN_TRANSIT);
        s.setStartedAt(LocalDateTime.now());
        if (s.getProgressPct() == null || s.getProgressPct() == 0.0) {
            s.setProgressPct(5.0);
        }

        Vehicle v = s.getVehicle();
        v.setStatus(VehicleStatus.ON_TRIP);
        vehicleRepository.save(v);

        s = shipmentRepository.save(s);
        auditLogService.recordAudit(currentUser, "SHIPMENT_STARTED", "SHIPMENT", s.getId(), "Started shipment " + s.getCode(), "127.0.0.1");
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));
        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto pauseShipment(Long id, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        validateDriverOrModeratorAccess(s, currentUser);

        if (s.getStatus() != ShipmentStatus.IN_TRANSIT && s.getStatus() != ShipmentStatus.DELAYED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot pause shipment in status " + s.getStatus());
        }

        s.setStatus(s.getStatus() == ShipmentStatus.IN_TRANSIT ? ShipmentStatus.DELAYED : ShipmentStatus.IN_TRANSIT);

        s = shipmentRepository.save(s);
        auditLogService.recordAudit(currentUser, "SHIPMENT_PAUSED", "SHIPMENT", s.getId(), "Updated shipment " + s.getCode() + " status to " + s.getStatus(), "127.0.0.1");
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));
        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto deliverShipment(Long id, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        validateDriverOrModeratorAccess(s, currentUser);

        if (s.getStatus() != ShipmentStatus.IN_TRANSIT && s.getStatus() != ShipmentStatus.DELAYED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot mark delivered from status " + s.getStatus());
        }

        s.setStatus(ShipmentStatus.DELIVERED);
        s.setDeliveredAt(LocalDateTime.now());
        s.setProgressPct(100.0);

        if (s.getVehicle() != null) {
            Vehicle v = s.getVehicle();
            v.setStatus(VehicleStatus.IDLE);
            if (s.getDestinationNode() != null) {
                v.setLat(s.getDestinationNode().getLat());
                v.setLng(s.getDestinationNode().getLng());
                v.setSpeedKmph(0.0);
            }
            vehicleRepository.save(v);
        }

        // Add weight to destination district stock
        if (s.getDestinationNode() != null && s.getDestinationNode().getDistrict() != null) {
            District destDistrict = s.getDestinationNode().getDistrict();
            CargoCategory category = s.getCargoCategory();

            DistrictStock stock = districtStockRepository.findByDistrictIdAndCategory(destDistrict.getId(), category)
                    .orElseGet(() -> new DistrictStock(destDistrict, category, 0.0, 100.0));

            stock.setStockUnits(stock.getStockUnits() + s.getWeightKg());
            stock.setUpdatedAt(LocalDateTime.now());
            districtStockRepository.save(stock);
        }

        s = shipmentRepository.save(s);
        auditLogService.recordAudit(currentUser, "SHIPMENT_DELIVERED", "SHIPMENT", s.getId(), "Delivered shipment " + s.getCode(), "127.0.0.1");
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));
        return ShipmentDto.fromEntity(s);
    }

    @Transactional
    public ShipmentDto cancelShipment(Long id, User currentUser) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        if (s.getStatus() == ShipmentStatus.DELIVERED || s.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel terminal shipment");
        }

        s.setStatus(ShipmentStatus.CANCELLED);

        if (s.getVehicle() != null) {
            Vehicle v = s.getVehicle();
            v.setStatus(VehicleStatus.IDLE);
            vehicleRepository.save(v);
        }

        s = shipmentRepository.save(s);
        auditLogService.recordAudit(currentUser, "SHIPMENT_CANCELLED", "SHIPMENT", s.getId(), "Cancelled shipment " + s.getCode(), "127.0.0.1");
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus()));
        return ShipmentDto.fromEntity(s);
    }

    public RoutePlanDto getRouteProposal(Long shipmentId) {
        return routePlanRepository.findFirstByShipmentIdAndStatusOrderByIdDesc(shipmentId, RoutePlanStatus.PROPOSED)
                .map(RoutePlanDto::fromEntity)
                .orElse(null);
    }

    @Transactional
    public RoutePlanDto acceptRouteProposal(Long shipmentId, User currentUser) {
        Shipment s = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        validateDriverOrModeratorAccess(s, currentUser);

        RoutePlan proposed = routePlanRepository.findFirstByShipmentIdAndStatusOrderByIdDesc(shipmentId, RoutePlanStatus.PROPOSED)
                .orElseThrow(() -> new ResourceNotFoundException("No proposed route plan found for shipment " + shipmentId));

        List<RoutePlan> activePlans = routePlanRepository.findByShipmentIdAndStatus(shipmentId, RoutePlanStatus.ACTIVE);
        for (RoutePlan active : activePlans) {
            active.setStatus(RoutePlanStatus.SUPERSEDED);
            routePlanRepository.save(active);
        }

        proposed.setStatus(RoutePlanStatus.ACTIVE);
        RoutePlan saved = routePlanRepository.save(proposed);

        if (proposed.getEtaMin() != null) {
            s.setCurrentEta(LocalDateTime.now().plusMinutes(Math.round(proposed.getEtaMin())));
            shipmentRepository.save(s);
        }

        auditLogService.recordAudit(currentUser, "ROUTE_PROPOSAL_ACCEPTED", "SHIPMENT", s.getId(), "Accepted safer route plan #" + saved.getId(), "127.0.0.1");
        sseService.broadcast("shipment.updated", Map.of("id", s.getId(), "status", s.getStatus(), "routePlanId", saved.getId()));
        return RoutePlanDto.fromEntity(saved);
    }

    @Transactional
    public RoutePlanDto rejectRouteProposal(Long shipmentId, User currentUser) {
        Shipment s = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        validateDriverOrModeratorAccess(s, currentUser);

        RoutePlan proposed = routePlanRepository.findFirstByShipmentIdAndStatusOrderByIdDesc(shipmentId, RoutePlanStatus.PROPOSED)
                .orElseThrow(() -> new ResourceNotFoundException("No proposed route plan found for shipment " + shipmentId));

        proposed.setStatus(RoutePlanStatus.REJECTED);
        RoutePlan saved = routePlanRepository.save(proposed);

        auditLogService.recordAudit(currentUser, "ROUTE_PROPOSAL_REJECTED", "SHIPMENT", s.getId(), "Rejected route proposal #" + saved.getId(), "127.0.0.1");
        return RoutePlanDto.fromEntity(saved);
    }

    private void validateDriverOrModeratorAccess(Shipment s, User currentUser) {
        boolean isModeratorOrAdmin = currentUser.getRole() == Role.MODERATOR || currentUser.getRole() == Role.ADMIN;
        boolean isDriver = s.getDriverUser() != null && s.getDriverUser().getId().equals(currentUser.getId());
        if (!isModeratorOrAdmin && !isDriver) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned driver or a moderator/admin can perform this action");
        }
    }
}
