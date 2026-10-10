package com.nerlogix.entity;

import com.nerlogix.model.enums.RoutePlanStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "route_plans")
public class RoutePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "segment_ids", columnDefinition = "TEXT")
    private String segmentIds;

    @Column(columnDefinition = "TEXT")
    private String geometry;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "eta_min", nullable = false)
    private Double etaMin;

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutePlanStatus status = RoutePlanStatus.PROPOSED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RoutePlan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getSegmentIds() { return segmentIds; }
    public void setSegmentIds(String segmentIds) { this.segmentIds = segmentIds; }

    public String getGeometry() { return geometry; }
    public void setGeometry(String geometry) { this.geometry = geometry; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Double getEtaMin() { return etaMin; }
    public void setEtaMin(Double etaMin) { this.etaMin = etaMin; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public RoutePlanStatus getStatus() { return status; }
    public void setStatus(RoutePlanStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
