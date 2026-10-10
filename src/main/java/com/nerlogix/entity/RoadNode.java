package com.nerlogix.entity;

import com.nerlogix.model.enums.NodeKind;
import jakarta.persistence.*;

@Entity
@Table(name = "road_nodes")
public class RoadNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeKind kind;

    public RoadNode() {}

    public RoadNode(String name, Double lat, Double lng, District district, NodeKind kind) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.district = district;
        this.kind = kind;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public NodeKind getKind() { return kind; }
    public void setKind(NodeKind kind) { this.kind = kind; }
}
