package com.nerlogix.entity;

import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.SegmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "road_segments")
public class RoadSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "from_node", nullable = false)
    private RoadNode fromNode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "to_node", nullable = false)
    private RoadNode toNode;

    @Column(nullable = false)
    private String name;

    @Column(name = "highway_code")
    private String highwayCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "length_km", nullable = false)
    private Double lengthKm;

    @Column(name = "base_speed_kmph", nullable = false)
    private Double baseSpeedKmph;

    @Column(name = "terrain_class", nullable = false)
    private Integer terrainClass;

    @Column(name = "landslide_prone")
    private Boolean landslideProne = false;

    @Column(name = "flood_prone")
    private Boolean floodProne = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentStatus status = SegmentStatus.OPEN;

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @Column(name = "risk_factors", columnDefinition = "TEXT")
    private String riskFactors;

    @Column(name = "status_reason", columnDefinition = "TEXT")
    private String statusReason;

    @Column(columnDefinition = "TEXT")
    private String polyline;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public RoadSegment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RoadNode getFromNode() { return fromNode; }
    public void setFromNode(RoadNode fromNode) { this.fromNode = fromNode; }

    public RoadNode getToNode() { return toNode; }
    public void setToNode(RoadNode toNode) { this.toNode = toNode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHighwayCode() { return highwayCode; }
    public void setHighwayCode(String highwayCode) { this.highwayCode = highwayCode; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public Double getLengthKm() { return lengthKm; }
    public void setLengthKm(Double lengthKm) { this.lengthKm = lengthKm; }

    public Double getBaseSpeedKmph() { return baseSpeedKmph; }
    public void setBaseSpeedKmph(Double baseSpeedKmph) { this.baseSpeedKmph = baseSpeedKmph; }

    public Integer getTerrainClass() { return terrainClass; }
    public void setTerrainClass(Integer terrainClass) { this.terrainClass = terrainClass; }

    public Boolean getLandslideProne() { return landslideProne; }
    public void setLandslideProne(Boolean landslideProne) { this.landslideProne = landslideProne; }

    public Boolean getFloodProne() { return floodProne; }
    public void setFloodProne(Boolean floodProne) { this.floodProne = floodProne; }

    public SegmentStatus getStatus() { return status; }
    public void setStatus(SegmentStatus status) { this.status = status; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }

    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }

    public String getPolyline() { return polyline; }
    public void setPolyline(String polyline) { this.polyline = polyline; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
