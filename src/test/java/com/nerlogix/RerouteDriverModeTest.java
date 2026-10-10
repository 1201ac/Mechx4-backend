package com.nerlogix;

import com.nerlogix.dto.RoutePlanDto;
import com.nerlogix.entity.RoutePlan;
import com.nerlogix.entity.Shipment;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.RoutePlanStatus;

import com.nerlogix.repository.RoutePlanRepository;
import com.nerlogix.repository.ShipmentRepository;
import com.nerlogix.repository.UserRepository;
import com.nerlogix.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RerouteDriverModeTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private RoutePlanRepository routePlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRouteProposalAcceptance() {
        User admin = userRepository.findByEmail("admin@nerlogix.com").orElseThrow();
        Shipment shipment = shipmentRepository.findAll().stream().findFirst().orElseThrow();

        // Create a active plan and a proposed plan for testing
        RoutePlan active = new RoutePlan();
        active.setShipment(shipment);
        active.setDistanceKm(120.0);
        active.setEtaMin(180.0);
        active.setStatus(RoutePlanStatus.ACTIVE);
        routePlanRepository.save(active);

        RoutePlan proposed = new RoutePlan();
        proposed.setShipment(shipment);
        proposed.setDistanceKm(135.0);
        proposed.setEtaMin(160.0);
        proposed.setReason("Bypasses blocked corridor");
        proposed.setStatus(RoutePlanStatus.PROPOSED);
        routePlanRepository.save(proposed);

        // Fetch proposal
        RoutePlanDto propDto = shipmentService.getRouteProposal(shipment.getId());
        assertThat(propDto).isNotNull();
        assertThat(propDto.id()).isEqualTo(proposed.getId());

        // Accept proposal
        RoutePlanDto accepted = shipmentService.acceptRouteProposal(shipment.getId(), admin);
        assertThat(accepted.status()).isEqualTo(RoutePlanStatus.ACTIVE);

        RoutePlan oldActive = routePlanRepository.findById(active.getId()).orElseThrow();
        assertThat(oldActive.getStatus()).isEqualTo(RoutePlanStatus.SUPERSEDED);
    }
}
