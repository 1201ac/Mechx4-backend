package com.nerlogix.service;

import com.nerlogix.dto.AssignShipmentRequest;
import com.nerlogix.dto.CreateShipmentRequest;

import com.nerlogix.entity.RoadNode;
import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.User;
import com.nerlogix.entity.Vehicle;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ShipmentStateMachineTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoadNodeRepository roadNodeRepository;

    private User admin;
    private User driver;
    private Vehicle vehicle;
    private RoadNode orig;
    private RoadNode dest;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(new User("admin_test@nerlogix.com", "pass", "Admin", Role.ADMIN));
        driver = userRepository.save(new User("driver_test@nerlogix.com", "pass", "Driver", Role.USER));
        vehicle = vehicleRepository.save(new Vehicle("TEST-REG-01", VehicleType.TRUCK, 3000.0, 26.0, 91.0));

        RoadNode n1 = roadNodeRepository.findAll().get(0);
        RoadNode n2 = roadNodeRepository.findAll().get(1);
        orig = n1;
        dest = n2;
    }

    @Test
    void testShipmentWorkflowSuccess() {
        // Create draft
        CreateShipmentRequest req = new CreateShipmentRequest(
                "SHP-TEST-1", CargoCategory.MEDICINE, "Test Medicine", 1000.0, ShipmentPriority.HIGH,
                orig.getId(), dest.getId(), null, null, null
        );
        var dto = shipmentService.createShipment(req, admin);
        assertThat(dto.status()).isEqualTo(ShipmentStatus.DRAFT);

        // Assign
        dto = shipmentService.assignShipment(dto.id(), new AssignShipmentRequest(vehicle.getId(), driver.getId()), admin);
        assertThat(dto.status()).isEqualTo(ShipmentStatus.ASSIGNED);

        // Start
        dto = shipmentService.startShipment(dto.id(), driver);
        assertThat(dto.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(vehicleRepository.findById(vehicle.getId()).get().getStatus()).isEqualTo(VehicleStatus.ON_TRIP);

        // Deliver
        dto = shipmentService.deliverShipment(dto.id(), driver);
        assertThat(dto.status()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(dto.progressPct()).isEqualTo(100.0);
        assertThat(vehicleRepository.findById(vehicle.getId()).get().getStatus()).isEqualTo(VehicleStatus.IDLE);
    }

    @Test
    void testInvalidTransitionThrowsConflict() {
        CreateShipmentRequest req = new CreateShipmentRequest(
                "SHP-TEST-2", CargoCategory.FOOD, "Ration", 500.0, ShipmentPriority.NORMAL,
                orig.getId(), dest.getId(), null, null, null
        );
        var dto = shipmentService.createShipment(req, admin);

        // Cannot deliver directly from DRAFT
        assertThatThrownBy(() -> shipmentService.deliverShipment(dto.id(), admin))
                .isInstanceOf(ResponseStatusException.class);
    }
}
