package com.nerlogix.entity;

import com.nerlogix.model.enums.CargoCategory;
import com.nerlogix.model.enums.ShipmentPriority;
import com.nerlogix.model.enums.ShipmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_category", nullable = false)
    private CargoCategory cargoCategory;

    @Column(name = "cargo_description", columnDefinition = "TEXT")
    private String cargoDescription;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentPriority priority = ShipmentPriority.NORMAL;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "origin_node", nullable = false)
    private RoadNode originNode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_node", nullable = false)
    private RoadNode destinationNode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_user_id")
    private User driverUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.DRAFT;

    @Column(name = "planned_eta")
    private LocalDateTime plannedEta;

    @Column(name = "current_eta")
    private LocalDateTime currentEta;

    @Column(name = "progress_pct")
    private Double progressPct = 0.0;

    @Column(name = "delay_min")
    private Integer delayMin = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Shipment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public CargoCategory getCargoCategory() { return cargoCategory; }
    public void setCargoCategory(CargoCategory cargoCategory) { this.cargoCategory = cargoCategory; }

    public String getCargoDescription() { return cargoDescription; }
    public void setCargoDescription(String cargoDescription) { this.cargoDescription = cargoDescription; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public ShipmentPriority getPriority() { return priority; }
    public void setPriority(ShipmentPriority priority) { this.priority = priority; }

    public RoadNode getOriginNode() { return originNode; }
    public void setOriginNode(RoadNode originNode) { this.originNode = originNode; }

    public RoadNode getDestinationNode() { return destinationNode; }
    public void setDestinationNode(RoadNode destinationNode) { this.destinationNode = destinationNode; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public User getDriverUser() { return driverUser; }
    public void setDriverUser(User driverUser) { this.driverUser = driverUser; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public LocalDateTime getPlannedEta() { return plannedEta; }
    public void setPlannedEta(LocalDateTime plannedEta) { this.plannedEta = plannedEta; }

    public LocalDateTime getCurrentEta() { return currentEta; }
    public void setCurrentEta(LocalDateTime currentEta) { this.currentEta = currentEta; }

    public Double getProgressPct() { return progressPct; }
    public void setProgressPct(Double progressPct) { this.progressPct = progressPct; }

    public Integer getDelayMin() { return delayMin; }
    public void setDelayMin(Integer delayMin) { this.delayMin = delayMin; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
