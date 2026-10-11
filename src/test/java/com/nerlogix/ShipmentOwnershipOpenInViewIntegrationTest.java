package com.nerlogix;

import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.User;
import com.nerlogix.entity.Vehicle;
import com.nerlogix.model.enums.VehicleStatus;
import com.nerlogix.model.enums.VehicleType;
import com.nerlogix.repository.ShipmentRepository;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.repository.VehicleRepository;
import com.nerlogix.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ShipmentOwnershipOpenInViewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String adminToken;
    private String userToken;
    private Long shipmentId;

    @BeforeEach
    void setup() {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();
        adminToken = tokenProvider.generateAccessToken(admin);

        User user = userRepository.findByEmail("user@nerlogix.com").orElseThrow();
        userToken = tokenProvider.generateAccessToken(user);

        // Ensure user owns at least one vehicle
        Vehicle v = new Vehicle("AS-01-TEST-" + System.currentTimeMillis() % 10000, VehicleType.TRUCK, 3000.0, 26.1, 91.7);
        v.setOwnerUser(user);
        v.setStatus(VehicleStatus.IDLE);
        vehicleRepository.save(v);

        Shipment s = shipmentRepository.findAll().stream().findFirst().orElse(null);
        if (s != null) {
            shipmentId = s.getId();
        }
    }

    @Test
    void testShipmentsListingReturns200WithOpenInViewFalse() throws Exception {
        mockMvc.perform(get("/api/shipments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testShipmentDetailReturns200WithOpenInViewFalse() throws Exception {
        if (shipmentId != null) {
            mockMvc.perform(get("/api/shipments/" + shipmentId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(shipmentId));
        }
    }

    @Test
    void testDispatchBoardReturns200WithOpenInViewFalse() throws Exception {
        mockMvc.perform(get("/api/dispatch/board")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testVehiclesListingReturns200WithOpenInViewFalse() throws Exception {
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testUserOwnedVehiclesReturns200WithOpenInViewFalse() throws Exception {
        mockMvc.perform(get("/api/users/me/vehicles")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
