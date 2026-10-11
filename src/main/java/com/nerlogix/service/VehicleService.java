package com.nerlogix.service;

import com.nerlogix.dto.*;
import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.User;
import com.nerlogix.entity.Vehicle;
import com.nerlogix.entity.VehiclePosition;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.ShipmentStatus;
import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.model.enums.VehicleType;
import com.nerlogix.repository.ShipmentRepository;
import com.nerlogix.repository.VehiclePositionRepository;
import com.nerlogix.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehiclePositionRepository vehiclePositionRepository;
    private final ShipmentRepository shipmentRepository;
    private final SseService sseService;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VehiclePositionRepository vehiclePositionRepository,
            ShipmentRepository shipmentRepository,
            SseService sseService) {
        this.vehicleRepository = vehicleRepository;
        this.vehiclePositionRepository = vehiclePositionRepository;
        this.shipmentRepository = shipmentRepository;
        this.sseService = sseService;
    }

    public Page<VehicleDto> getVehicles(String status, String type, String q, Pageable pageable) {
        VehicleStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = VehicleStatus.valueOf(status.toUpperCase()); } catch (Exception ignored) {}
        }
        VehicleType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try { typeEnum = VehicleType.valueOf(type.toUpperCase()); } catch (Exception ignored) {}
        }

        final VehicleStatus fStatus = statusEnum;
        final VehicleType fType = typeEnum;
        final String fQ = (q != null && !q.isBlank()) ? q.toLowerCase() : null;

        org.springframework.data.jpa.domain.Specification<Vehicle> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (fStatus != null) {
                predicates.add(cb.equal(root.get("status"), fStatus));
            }
            if (fType != null) {
                predicates.add(cb.equal(root.get("type"), fType));
            }
            if (fQ != null) {
                predicates.add(cb.like(cb.lower(root.get("regNo")), "%" + fQ + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return vehicleRepository.findAll(spec, pageable).map(VehicleDto::fromEntity);
    }

    public VehicleDto getVehicleById(Long id) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
        return VehicleDto.fromEntity(v);
    }

    @Transactional
    public VehicleDto createVehicle(CreateVehicleRequest req) {
        if (vehicleRepository.findByRegNo(req.regNo()).isPresent()) {
            throw new IllegalArgumentException("Vehicle registration number already exists: " + req.regNo());
        }
        Vehicle v = new Vehicle(req.regNo(), req.type(), req.capacityKg(), req.lat(), req.lng());
        if (req.status() != null) {
            v.setStatus(req.status());
        }
        v = vehicleRepository.save(v);
        return VehicleDto.fromEntity(v);
    }

    @Transactional
    public VehicleDto updateVehicle(Long id, UpdateVehicleRequest req) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));

        if (req.regNo() != null && !req.regNo().isBlank()) {
            v.setRegNo(req.regNo());
        }
        if (req.type() != null) v.setType(req.type());
        if (req.capacityKg() != null) v.setCapacityKg(req.capacityKg());
        if (req.status() != null) v.setStatus(req.status());
        if (req.lat() != null) v.setLat(req.lat());
        if (req.lng() != null) v.setLng(req.lng());
        if (req.heading() != null) v.setHeading(req.heading());
        if (req.speedKmph() != null) v.setSpeedKmph(req.speedKmph());
        v.setLastSeen(LocalDateTime.now());

        v = vehicleRepository.save(v);
        return VehicleDto.fromEntity(v);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
        vehicleRepository.delete(v);
    }

    public List<VehiclePositionDto> getVehicleTrail(Long id) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
        List<VehiclePosition> positions = vehiclePositionRepository.findTop20ByVehicleIdOrderByRecordedAtDesc(v.getId());
        return positions.stream().map(VehiclePositionDto::fromEntity).toList();
    }

    @Transactional
    public void recordDriverPositions(User driver, List<VehiclePositionRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        Optional<Shipment> activeShipmentOpt = shipmentRepository.findFirstByDriverUserIdAndStatusIn(
                driver.getId(),
                List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELAYED, ShipmentStatus.ASSIGNED)
        );

        if (activeShipmentOpt.isEmpty()) {
            return;
        }

        Shipment activeShipment = activeShipmentOpt.get();
        Vehicle vehicle = activeShipment.getVehicle();
        if (vehicle == null) return;

        for (VehiclePositionRequest req : requests) {
            LocalDateTime timestamp = req.recordedAt() != null ? req.recordedAt() : LocalDateTime.now();
            vehicle.setLat(req.lat());
            vehicle.setLng(req.lng());
            if (req.heading() != null) vehicle.setHeading(req.heading());
            if (req.speedKmph() != null) vehicle.setSpeedKmph(req.speedKmph());
            vehicle.setLastSeen(timestamp);
            vehicle.setLastDriverFixAt(LocalDateTime.now());

            VehiclePosition pos = new VehiclePosition(vehicle, req.lat(), req.lng(), req.heading() != null ? req.heading() : 0.0, req.speedKmph() != null ? req.speedKmph() : 0.0);
            pos.setRecordedAt(timestamp);
            vehiclePositionRepository.save(pos);
        }

        vehicleRepository.save(vehicle);

        sseService.broadcast("vehicle.update", Map.of(
                "vehicleId", vehicle.getId(),
                "regNo", vehicle.getRegNo(),
                "lat", vehicle.getLat(),
                "lng", vehicle.getLng(),
                "heading", vehicle.getHeading(),
                "speedKmph", vehicle.getSpeedKmph(),
                "lastSeen", vehicle.getLastSeen()
        ));
    }
}
